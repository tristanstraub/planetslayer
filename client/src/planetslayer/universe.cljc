(ns planetslayer.universe)

(defn id! []
  (defonce ids (atom 0))
  (swap! ids inc))

(defn object! [type & {:keys [tag pos material update radius scale model rotate look-at]}]
  {:id       (id!)
   :type     type
   :tag      tag
   :pos      pos
   :material material
   :update   update
   :radius   radius
   :scale    scale
   :rotate   (or rotate [0 0 0])
   :model    model
   :look-at  look-at})

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

(defn v+ [a b]
  (mapv + a b))

(defn v* [s b]
  (mapv * (repeat s) b))

(defn get-dir [player]
  (let [rot        (:rotate player)]
    [(Math/sin (rot 1)) 0 (Math/cos (rot 1))]))

(defn look-through-player [camera player]
  ;;player-pos player-rotation player-right
  (let [look-at (get-dir player)]
    (assoc camera :look-at (v+ (:pos player) look-at))))

(defn get-player [app]
  (first (filter (comp #(= % :player) :tag) (-> app :universe :objects))))

(defn make-universe []
  {:objects [(object! :camera
                      :pos [0 0 10]
                      :look-at [0 0 0]

                      :update (fn [p time app time-delta]
                                (let [player (get-player app)]
                                  (cond-> p
                                    player
                                    (-> (assoc :pos (:pos player))
                                        (look-through-player player))))))

             (object! :ship
                      :tag :player
                      :pos [0 0 5]
                      :model "assets/ship.stl"
                      :scale [0.2 0.2 0.2]
                      :rotate [0 Math/PI Math/PI]
                      :update (fn [p time app time-delta]
                                (let [time-delta (* 0.01 (/ time-delta 2.0))
                                      player     (get-player app)
                                      dir        (get-dir player)]

                                  (cond-> p
                                    ;; true
                                    ;; (update :rotate #(v+ % [0 -0.1 0]))

                                    (get (:keys-state app) 16) ;; shift
                                    (update :pos #(v+ % (v* time-delta dir)))

                                    (get (:keys-state app) 17) ;; ctrl
                                    (update :pos #(v+ % (v* time-delta (v* -1 dir))))

                                    (get (:keys-state app) 65) ;; right - d
                                    (update :pos #(v+ % (v* time-delta [-1 0 0])))

                                    (get (:keys-state app) 68) ;; left - a
                                    (update :pos #(v+ % (v* time-delta [1 0 0])))

                                    (get (:keys-state app) 87) ;; up - w
                                    (update :pos #(v+ % (v* time-delta [0 1 0])))

                                    (get (:keys-state app) 83) ;; down - s
                                    (update :pos #(v+ % (v* time-delta [0 -1 0])))

                                    (get (:keys-state app) 69) ;; rot left - q
                                    (update :rotate #(v+ % (v* time-delta [0 -1 0])))

                                    (get (:keys-state app) 81) ;; rot right - e
                                    (update :rotate #(v+ % (v* time-delta [0 1 0]))))))
                      )
             (object! :ship
                      :pos [2 -2 -1]
                      :model "assets/ship.stl"
                      :scale [0.1 0.1 0.1]
                      :rotate [0 0 Math/PI]
                      :update (fn [p time app time-delta]
                                (assoc p
                                       :pos (v+ [(* 3 (Math/sin (/ time 10000)))
                                                 0
                                                 (* 3 (Math/cos (/ time 10000)))]
                                                [(* 1 (Math/sin (/ time 10000)))
                                                 0
                                                 (* 1 (Math/cos (/ time 10000)))]
                                                ))))
             (object! :planet :pos [0 0 0]
                      :radius 1
                      :material (material :image "images/sun.jpg"
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
