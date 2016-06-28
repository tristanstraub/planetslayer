(ns planetslayer.universes.planetslayer
  (:require [planetslayer.universe :refer [object! get-player get-dir look-through-player material]]
            [planetslayer.math :refer [m*v v+v v-norm v* ;; v^ v* v*v |v|
                                       identity-matrix rotation-matrix]]))

(defn make-universe []
  {:toolbar [ ;; toolbar
             (object! :camera
                      :camera-type :ortho
                      :pos [0 0 200]
                      :look-at [-0.75 0 0])

             ;; remaining
             ;; (object! :ship
             ;;          :pos [0 -0.2 1]
             ;;          :scale [0.1 0.1 1]
             ;;          :transparent true
             ;;          :opacity 0.5
             ;;          :model "assets/ship/cockpit-simple.json"
             ;;          :update (fn [p time app time-delta]
             ;;                    (println :update-toolbar-ship)
             ;;                    (assoc p :rotate [(/ time 100) 0 0])))

             ;; (object! :ship
             ;;          :pos [0.2 -0.2 1]
             ;;          :scale [0.1 0.1 1]
             ;;          :transparent true
             ;;          :opacity 0.5
             ;;          :model "assets/ship/cockpit-simple.json"
             ;;          :update (fn [p time app time-delta]
             ;;                    (println :update-toolbar-ship)
             ;;                    (assoc p :rotate [(/ time 100) 0 0])))

             (object! :ship
                      :pos [0.2 -0.2 1]
                      :scale [0.1 0.1 1]
                      :transparent true
                      :opacity 0.5
                      :model "assets/ship/cockpit-simple.json"
                      :update (fn [p time app time-delta]
                                (-> p
                                    (assoc :rotate [0 0 (/ time 100)])
                                    (assoc :pos (v* 0.2 [(Math/cos (/ time 300))
                                                         (* (Math/sin (/ time 300)) (/ 16 9)) 0])))))

             (object! :ship
                      :pos [0.2 -0.2 1]
                      :scale [0.1 0.1 1]
                      :transparent true
                      :opacity 0.5
                      :model "assets/ship/cockpit-simple.json"
                      :update (fn [p time app time-delta]
                                (-> p
                                    (assoc :rotate [0 0 (/ time 100)])
                                    (assoc :pos (v* 0.2 [(Math/cos (/ time -300))
                                                         (* (Math/sin (/ time -300)) (/ 16 9)) 0])))))

             (object! :ship
                      :pos [0 0 1]
                      :scale [0.1 0.1 1]
                      :transparent true
                      :opacity 0.5
                      :model "assets/ship/cockpit-simple.json"
                      :update (fn [p time app time-delta]
                                (-> p
                                    (assoc :scale (v* (Math/sin (/ time 100)) [0.1 0.1 0.1]))
)))]

   :objects [(object! :camera
                      :pos [0 0 5]
                      :look-at [0 0 0]

                      :update (fn [p time app time-delta]
                                (let [player (get-player app)
                                      dir    (get-dir player)
                                      norm (v-norm dir)]
                                  (cond-> p
                                    player
                                    (-> (assoc :pos (:pos player))
                                        (assoc :pos-offset (v* -20 (v-norm dir)))
                                        (update :pos-offset v+v [0 5 0])
                                        (assoc p :look-at (:pos player))
                                        (look-through-player player)))))
                      )

             (object! :ship
                      :tag :player
                      :transparent false
                      :opacity 0.5
                      :pos [0 0 10]
                      :model "assets/baseship1.json" ;;"assets/ship.stl"
                      :scale [1 1 1]
                      :rotate [0 (/ Math/PI 2) 0]
                      ;; :material (material :image "images/spaceship1.jpg")
                      :update (fn [p time app time-delta]
                                (let [rot-time-delta (* 0.01 (/ time-delta 16.0))
                                      time-delta (* 0.1 (/ time-delta 2.0))
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
                                    (update :rotate #(v+v % (v* rot-time-delta [0 -1 0])))

                                    (get (:keys-state app) 65) ;; right - d
                                    (update :rotate #(v+v % (v* rot-time-delta [0 1 0])))

                                    ;; (get (:keys-state app) 87) ;; up - w
                                    ;; (update :rotate #(v+v % (v* time-delta [0 0 1])))

                                    ;; (get (:keys-state app) 83) ;; down - s
                                    ;; (update :rotate #(v+v % (v* time-delta [-1 0 -1])))

                                    (get (:keys-state app) 69) ;; rot left - q
                                    ;; should rotate on the z-axis
                                    (update :rotate #(v+v % (v* rot-time-delta [0 0 1])))

                                    (get (:keys-state app) 81) ;; rot right - e
                                    ;; should rotate on the z-axis
                                    (update :rotate #(v+v % (v* rot-time-delta [0 0 -1])))))))

             ;; (object! :ship
             ;;          :pos [2 -2 -1]
             ;;          :model "assets/ship.stl"
             ;;          :scale [0.1 0.1 0.1]
             ;;          :rotate [0 0 Math/PI]
             ;;          :update (fn [p time app time-delta]
             ;;                    (assoc p
             ;;                           :pos (v+v [(* 3 (Math/sin (/ time 10000)))
             ;;                                      0
             ;;                                      (* 3 (Math/cos (/ time 10000)))]
             ;;                                     [(* 1 (Math/sin (/ time 10000)))
             ;;                                      0
             ;;                                      (* 1 (Math/cos (/ time 10000)))]
             ;;                                     ))))
             ;; (object! :planet :pos [0 0 0]
             ;;          :scale [100000 100000 100000]
             ;;          :radius 1
             ;;          :material (material :image "images/sun.jpg"
             ;;                              :color 0xaaaaaa)
             ;;          :update (fn [p time app time-delta]
             ;;                    (assoc p :rotate [0 (/ time -8000) 0])))
             (object! :planet :pos (v* -3000 [0 0 1])
                      :scale (v* 5000 [1 1 1])
                      :radius 0.5
                      :material (material :image "images/burning-planet.jpg")
                      ;; :update (fn [p time app time-delta]
                      ;;           (assoc p
                      ;;                  :pos [(* 3 (Math/sin (/ time 10000)))
                      ;;                        0
                      ;;                        (+ 30000 (* 3 (Math/cos (/ time 10000))))]))
                      )]})
