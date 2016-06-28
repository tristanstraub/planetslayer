(ns planetslayer.universes.jumpout
  (:require [planetslayer.universe :refer [object! get-player get-dir look-through-player material]]
            [planetslayer.math :refer [m*v v+v v-norm v* ;; v^ v* v*v |v|
                                       identity-matrix rotation-matrix]]))

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
        "x     x "
        "x      x "
        "x        "
        "x     p   "
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

(defn scale-axis [v]
  (* 0.1 v))

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

(defn update-player [p time app time-delta]
  (let [cell-width    0.3
        player        (get-player app)
        player-rect   (select-keys player [:pos :scale])
        {:keys [j i]} (:attributes player)
        pieces        (->> app :universe :objects (map #(select-keys % [:pos :scale]))
                           (filter :pos)
                           (filter :scale))]



    #_    (println :intersections (->> pieces
                                       (filter #(intersects-with player %))
                                       (filter #(not= % player-rect))))

    (let [{:keys [controller]} app
          new-pos              (-> (:pos p)
                                   ;; (v+v (v* cell-width [i (* -1 j) -1]))
                                   (v+v (v* (scale-axis (or (-> controller :left-joystick :horizontal) 0)) [1 0 0]))
                                   (v+v (v* (scale-axis (or (-> controller :left-joystick :vertical) 0)) [0 -1 0])))

          new-pos-h            (-> (:pos p)
                                   ;; (v+v (v* cell-width [i (* -1 j) -1]))
                                   (v+v (v* (scale-axis (or (-> controller :left-joystick :horizontal) 0)) [1 0 0])))
          new-pos-v            (-> (:pos p)
                                   ;; (v+v (v* cell-width [i (* -1 j) -1]))
                                   (v+v (v* (scale-axis (or (-> controller :left-joystick :vertical) 0)) [0 -1 0])))

          new-player-rect      {:pos new-pos :scale (:scale p)}
          new-player-rect-h    {:pos new-pos-h :scale (:scale p)}
          new-player-rect-v    {:pos new-pos-v :scale (:scale p)}

          old-player-rect      (select-keys p [:pos :scale])

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
            (assoc p :pos new-pos)

            (empty? h-intersections)
            (assoc p :pos new-pos-h)

            (empty? v-intersections)
            (assoc p :pos new-pos-v)

            :else
            p)

      ;; (player-falls app p)

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
