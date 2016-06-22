(ns planetslayer.universe
  (:require [planetslayer.math :refer [m*v v+v v-norm v* ;; v^ v* v*v |v|
                                       identity-matrix rotation-matrix]]))

(defn id! []
  (defonce ids (atom 0))
  (swap! ids inc))

(defn object! [type & {:keys [tag pos material update radius scale model rotate look-at transparent opacity]}]
  {:id          (id!)
   :type        type
   :tag         tag
   :pos         pos
   :material    material
   :update      update
   :radius      radius
   :scale       scale
   :rotate      (or rotate [0 0 0])
   :model       model
   :look-at     look-at
   :transparent transparent
   :opacity     opacity})

(defn material [& {:keys [color image]}]
  {:color color
   :image image})

(defn material-color [m]
  (:color m))

(defn material-image [m]
  (:image m))

(defn planets [u]
  (->> u :objects
       (filter #(= :planet (:type %)))))

(defn planet? [object]
  (= (:type object) :planet))

(defn camera? [object]
  (= (:type object) :camera))

(defn objects [universe]
  (:objects universe))

(defn get-dir [player]
  (m*v (rotation-matrix (:rotate player)) [1 0 0]))

(defn threejs->vec [r]
  [(.-x r) (.-y r) (.-z r)])

(defn look-through-player [camera player]
  (let [v       (get-dir player)]
    (assoc camera :look-at (v+v (:pos player) v))))

(defn get-player [app]
  (first (filter (comp #(= % :player) :tag) (-> app :universe :objects))))

(defn make-universe []
  {:objects [(object! :camera
                      :pos [0 0 10]
                      :look-at [0 0 0]

                      :update (fn [p time app time-delta]
                                (let [player (get-player app)
                                      dir    (get-dir player)
                                      norm (v-norm dir)]
                                  (cond-> p
                                    player
                                    (-> (assoc :pos (:pos player))
                                        (assoc :pos-offset (v* -1 (v-norm dir)))
                                        (look-through-player player)))))
                      )

             (object! :ship
                      :tag :player
                      :transparent false
                      :opacity 0.5
                      :pos [0 0 5]
                      :model "assets/baseship1.json" ;;"assets/ship.stl"
                      :scale [0.2 0.2 0.2]
                      :rotate [0 0 0]
                      ;; :material (material :image "images/spaceship1.jpg")
                      :update (fn [p time app time-delta]
                                (let [time-delta (* 0.01 (/ time-delta 2.0))
                                      player     (get-player app)
                                      dir        (get-dir player)]

                                  (cond-> p
                                    ;; true
                                    ;; (update :rotate #(v+v % [0 -0.1 0]))

                                    (get (:keys-state app) 16) ;; shift
                                    (update :pos #(v+v % (v* time-delta dir)))

                                    (get (:keys-state app) 17) ;; ctrl
                                    (update :pos #(v+v % (v* time-delta (v* -1 dir))))

                                    (get (:keys-state app) 68) ;; left - a
                                    (update :rotate #(v+v % (v* time-delta [0 -1 0])))

                                    (get (:keys-state app) 65) ;; right - d
                                    (update :rotate #(v+v % (v* time-delta [0 1 0])))

                                    ;; (get (:keys-state app) 87) ;; up - w
                                    ;; (update :rotate #(v+v % (v* time-delta [0 0 1])))

                                    ;; (get (:keys-state app) 83) ;; down - s
                                    ;; (update :rotate #(v+v % (v* time-delta [-1 0 -1])))

                                    (get (:keys-state app) 69) ;; rot left - q
                                    ;; should rotate on the z-axis
                                    (update :rotate #(v+v % (v* time-delta [0 0 1])))

                                    (get (:keys-state app) 81) ;; rot right - e
                                    ;; should rotate on the z-axis
                                    (update :rotate #(v+v % (v* time-delta [0 0 -1]))))))
                      )
             (object! :ship
                      :pos [2 -2 -1]
                      :model "assets/ship.stl"
                      :scale [0.1 0.1 0.1]
                      :rotate [0 0 Math/PI]
                      :update (fn [p time app time-delta]
                                (assoc p
                                       :pos (v+v [(* 3 (Math/sin (/ time 10000)))
                                                  0
                                                  (* 3 (Math/cos (/ time 10000)))]
                                                 [(* 1 (Math/sin (/ time 10000)))
                                                  0
                                                  (* 1 (Math/cos (/ time 10000)))]
                                                 ))))
             (object! :planet :pos [0 0 0]
                      :radius 1
                      :material (material :image "images/spaceship1.jpg"
                                          :color 0xaaaaaa)
                      :update (fn [p time app time-delta]
                                (assoc p :rotate [0 (/ time -8000) 0])))
             (object! :planet :pos [3 0 -5]
                      :radius 0.5
                      :material (material :image "images/burning-planet.jpg")
                      :update (fn [p time app time-delta]
                                (assoc p
                                       :pos [(* 3 (Math/sin (/ time 10000)))
                                             0
                                             (* 3 (Math/cos (/ time 10000)))])))]})
