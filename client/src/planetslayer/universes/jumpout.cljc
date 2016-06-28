(ns planetslayer.universes.jumpout
  (:require [planetslayer.universe :refer [object! get-player get-dir look-through-player material]]
            [planetslayer.math :refer [m*v v+v v-norm v* ;; v^ v* v*v |v|
                                       identity-matrix rotation-matrix
                                       sign]]))

(defn- vindex [row]
  (map-indexed vector row))

(defn pos->cell [pos]
  [0 0])

(defn player [& {:keys [pos]}]
  {:cell (pos->cell pos)
   :pos  pos})

(defn player? [p]
  (= p "p"))

(defn player-cell [world]
  (->> (vindex world)
       (mapcat (fn [[j row]]
                 (->> (vindex row)
                      (map (fn [[i cell]]
                             (when (player? cell)
                               [j i]))))))
       (remove nil?)
       first))

(defn make-world []
  (->> ["xxxxxxxxxx  xxxxxxxx "
        "x      x "
        "x      x "
        "x      x "
        "x      x "
        "x        "
        "x        "
        "x        "
        "xxxxxxxxxx  xxxxxxxx "
        "x      x "
        "x     x  "
        "x     x   p"
        "x      x "
        "x        "
        "x        "
        "x        "
        "xx~~~xxxxxxxxxx "
        "         "]
       (mapv #(into [] %))))

(defn- player-boundary-cells [world player]
  (let [cell-width    0.3
        room-size     2
        player-height 0.6
        player-width  0.3
        [x y z]       (:pos player)]
    (v+v (:pos player) [(/ player-width 2) 0 0])
    (v+v (:pos player) [(/ player-height 2) 0 0])

    {:left   (Math/floor (/ x cell-width))
     :top    (Math/floor (- (/ y cell-width)))}))

(defn on-floor? [app player]
  (let [world              (get-in app [:universe :world])
        {:keys [left top]} (player-boundary-cells world player)]
    ;; (println bottom top left :cell (get-in world [top left]))
    ;; (println :bottom bottom (get-in world [bottom left]))
    (or (= \x (get-in world [top left]))
        ;; (= \x (get-in world [bottom left]))
        )))

(defn- player-falls [app p]
  (if-not (on-floor? app p)
    (update p :pos #(v+v % [0 -0.04 0]))
    p))

(defn intersects-with [player piece]
  ;; intersect parallel rectangles
  ;; |---|
  ;; |   |
  ;; | |-|--|
  ;; |---|  |
  ;;   |    |
  ;;   |----|
  ;; 1. separating axis
  (let [[a b]                       [player piece]
        half                        (fn [x] (/ x 2))
        width                       (fn [x] ((:scale x) 0))
        height                      (fn [x] ((:scale x) 1))
        left                        (fn [x] ((:pos x) 0))
        bottom                      (fn [x] (- ((:pos x) 1) (half (height x))))
        right                       (fn [x] (+ (left x) (width x)))
        top                         (fn [x] (+ (bottom x) (height x)))

        [lefta righta topa bottoma] [(left a) (right a) (top a) (bottom a)]
        [leftb rightb topb bottomb] [(left b) (right b) (top b) (bottom b)]

        separate                    (or (< righta leftb)
                                        (< rightb lefta)
                                        (< topb bottoma)
                                        (< topa bottomb))]
    (not separate)))

(defn can-move-in-direction? [player app dir]
  (let [new-pos         (-> (:pos player) (v+v dir))
        new-player-rect {:pos new-pos :scale (:scale player)}
        old-player-rect (select-keys player [:pos :scale])
        pieces        (->> app :universe :objects (map #(select-keys % [:pos :scale]))
                           (filter :pos)
                           (filter :scale))
        intersections   (->> pieces
                             (filter #(intersects-with new-player-rect %))
                             (filter #(not= % old-player-rect)))]
    (empty? intersections)))

(defn move-smooth-in-direction [player time app time-delta dir]
  ;; TODO prevent falling through close objects (limit time-delta or make it always the same
  ;;      size)
  (let [cell-width    0.3
        player-rect   (select-keys player [:pos :scale])
        {:keys [j i]} (:attributes player)
        pieces        (->> app :universe :objects (map #(select-keys % [:pos :scale]))
                           (filter :pos)
                           (filter :scale))

        dir-h         [(dir 0) 0 (dir 2)]
        dir-v         [0 (dir 1) (dir 2)]]

    (let [{:keys [controller]} app
          new-pos              (-> (:pos player) (v+v dir-h) (v+v dir-v))
          new-pos-h            (-> (:pos player) (v+v dir-h))
          new-pos-v            (-> (:pos player) (v+v dir-v))

          new-player-rect      {:pos new-pos :scale (:scale player)}
          new-player-rect-h    {:pos new-pos-h :scale (:scale player)}
          new-player-rect-v    {:pos new-pos-v :scale (:scale player)}

          old-player-rect      (select-keys player [:pos :scale])

          q-intersections      (->> pieces
                                    (filter #(intersects-with new-player-rect %))
                                    (filter #(not= % old-player-rect)))

          h-intersections      (->> pieces
                                    (filter #(intersects-with new-player-rect-h %))
                                    (filter #(not= % old-player-rect)))

          v-intersections      (->> pieces
                                    (filter #(intersects-with new-player-rect-v %))
                                    (filter #(not= % old-player-rect)))]

      (cond (empty? q-intersections)
            (assoc player :pos new-pos)

            (empty? h-intersections)
            (-> player
                (assoc :pos new-pos-h)
                (assoc-in [:velocity 1] 0))

            (empty? v-intersections)
            (-> player
                (assoc :pos new-pos-v)
                (assoc-in [:velocity 0] 0))

            :else
            (assoc player :velocity [0 0 0])))))

(defn update-player [player time app time-delta]
  (let [{:keys [controller]} app
        scale-axis           (fn [v] (* 0.0004 v))
        max-v                (fn [x] (if (> (Math/abs x) 0.0003) (* (sign x) 0.0003) x))
        ;; cap the player velocity at a maximum
        max-travel           (fn [v] (mapv max-v v))
        dampen-v             (fn [x k] (cond (> (Math/abs x) (* k 100))
                                             x

                                             (> (Math/abs x) k)
                                             (- x (* k (sign x)))

                                             :else
                                             0))
        ;; dampen player movement
        dampen               (fn [v k] [(dampen-v (v 0) k) (dampen-v (v 1) k) (v 2)])]


    (println (can-move-in-direction?  player app [0 -0.01 0]))

    (-> player
        ;; button-jump
        (cond->
            (and (get-in controller [:buttons 0 :pressed])
                 (or (nil? (:unpressed player))
                     (= (:unpressed player) true))
                 ;; vertical velocity is almost zero
                 (and (< (Math/abs ((:velocity player) 1)) 0.001)
                      (not (can-move-in-direction?  player app [0 -0.01 0]))))
            (-> (update :velocity v+v [0 0.002 0])
                ;; disable flying
                (assoc :unpressed false))

            ;; reenable jump
            (and (not (get-in controller [:buttons 0 :pressed])))
            (assoc :unpressed true))

        (update :velocity
                #(-> %
                     (v+v (dampen (max-travel (v* (scale-axis (or (-> controller :left-joystick :horizontal) 0)) [1 0 0])
                                              ;; (v* (scale-axis (or (-> controller :left-joystick :vertical) 0)) [0 -10 0])
                                              )
                                  0.00001))
                     #_(dampen 0.0001)))
        ;; left-joystick movement
        (update :velocity
                #(-> %
                     (v+v (dampen (max-travel (v* (scale-axis (or (-> controller :left-joystick :horizontal) 0)) [1 0 0])
                                              ;; (v* (scale-axis (or (-> controller :left-joystick :vertical) 0)) [0 -10 0])
                                              )
                                  0.00001))
                     (dampen 0.0001)))
        ;; gravity
        (update :velocity v+v [0 (* time-delta -0.00001) 0])

        ;; apply velocity
        (as-> player
            (move-smooth-in-direction player time app time-delta (v* time-delta (:velocity player))
                                      ))
        ;; (move-smooth-in-direction time app time-delta [0 (* time-delta -0.005) 0])
        ;; (move-smooth-in-direction time app time-delta
        ;;                           (-> (v* (scale-axis (or (-> controller :left-joystick :horizontal) 0)) [1 0 0])
        ;;                               (v+v (v* (scale-axis (or (-> controller :left-joystick :vertical) 0)) [0 -1 0]))))

        ;; gravity -- IDEA: could be attractive to only certain blocks
        )))


(defn objectify-world! [rows]
  (let [cell-width    0.3
        room-size     2
        player-height 0.6
        player-width  0.3]
    (->> (vindex rows)
         (mapcat (fn [[j row]]
                   (map (fn [[i cell]]
                          (case cell
                            \~ (object! :mesh
                                        :mesh-type :plane
                                        :material (material :color 0xff0000)
                                        :scale (v* cell-width [1 0.4 1])
                                        :pos (v* cell-width [i (+ -0.3 (* -1 j)) -1])
                                        :update (fn [p time app time-delta]
                                                  (assoc p :pos-offset (v* (* 0.02 (Math/random)) [1 1 1]))

                                                  )
                                        )

                            \x (object! :mesh
                                        :mesh-type :plane
                                        :material (material :color 0x0000ff)
                                        :scale (v* cell-width [1 1 1])
                                        :pos (v* cell-width [i (* -1 j) -1])
                                        :update (fn [p time app time-delta]
                                                  ;; (assoc p :pos-offset (v* (* 0.01 (Math/random)) [1 1 1]))
                                                  (assoc-in p [:material :color]
                                                            (if (intersects-with p (get-player app))
                                                              0xff0000
                                                              0x0000ff))
                                                  )
                                        )
                            \p (object! :mesh
                                        :tag :player
                                        :attributes {:i i :j j}
                                        :mesh-type :plane
                                        :material (material :color 0x00ff00)
                                        :scale [player-width player-height 1]
                                        :pos (v* cell-width [i (* -1 j) -1])
                                        :update update-player)
                            nil))
                        (vindex row))))

         (into []))))

(defn make-universe []
  (let [room-size     2
        player-height 0.6
        player-width  0.3
        world         (make-world)]

    {:toolbar [(object! :camera
                        :camera-type :ortho
                        :pos [0 0 200]
                        :look-at [-0.75 0 0])]

     :world world

     :objects (apply conj (objectify-world! world)
                     [
                      (object! :camera
                               :pos [0 0 5]
                               :look-at [0 0 0]

                               :update (fn [p time app time-delta]
                                         (let [player     (get-player app)
                                               cam-pos    (assoc-in (:pos player) [2] 5)
                                               lookat-pos (-> (:pos player)
                                                              (assoc-in [2] 0)
                                                              (v+v [-2 1 0]))]
                                           (cond-> p
                                             player
                                             (-> (assoc :pos cam-pos)
                                                 (assoc :look-at lookat-pos)
                                                 ;; (assoc :pos-offset (v* -20 (v-norm dir)))
                                                 ;; (update :pos-offset v+v [0 5 0])
                                                 ;; (assoc p :look-at (:pos player))
                                                 ;; (look-through-player player)
                                                 ))))
                               )
                      ;; (object! :mesh
                      ;;          :mesh-type :plane
                      ;;          :material (material :color 0xff0000)
                      ;;          :scale (v* room-size [0.25 1.75 1])
                      ;;          :pos (v* room-size [0.75 0 0]))

                      ;; (object! :mesh
                      ;;          :mesh-type :plane
                      ;;          :material (material :color 0xff0000)
                      ;;          :scale (v* room-size [1.75 0.25 1])
                      ;;          :pos (v* room-size  [0 0.75 0]))

                      ;; (object! :mesh
                      ;;          :mesh-type :plane
                      ;;          :material (material :color 0xff0000)
                      ;;          :scale (v* room-size [0.25 1.75 1])
                      ;;          :pos (v* room-size  [-0.75 0 0]))

                      ;; (object! :mesh
                      ;;          :mesh-type :plane
                      ;;          :material (material :color 0xff0000)
                      ;;          :scale (v* room-size [1.75 0.25 1])
                      ;;          :pos (v* room-size  [0 -0.75 0]))

                      ])}))
