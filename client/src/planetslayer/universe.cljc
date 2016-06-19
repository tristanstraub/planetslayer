(ns planetslayer.universe)

(defn id! []
  (defonce ids (atom 0))
  (swap! ids inc))

(defn object! [type & {:keys [pos material update radius scale model rotate]}]
  {:id       (id!)
   :type     type
   :pos      pos
   :material material
   :update   update
   :radius   radius
   :scale    scale
   :rotate   rotate
   :model    model})

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

(defn objects [universe]
  (:objects universe))

(defn v+ [a b]
  (mapv + a b))

(defn v* [s b]
  (mapv * (repeat s) b))

(defn make-universe []
  {:objects [(object! :ship
                      :pos [-2 -2 -1]
                      :model "assets/ship.stl"
                      :scale [0.2 0.2 0.2]
                      :rotate [(/ Math/PI 2) 0 0]
                      :update (fn [p time app time-delta]
                                (let [time-delta (/ time-delta 2.0)]
                                  (cond-> p
                                    (get (:keys-state app) 65) ;; right - d
                                    (update :pos #(v+ % (v* time-delta [-0.01 0 0])))

                                    (get (:keys-state app) 68) ;; left - a
                                    (update :pos #(v+ % (v* time-delta [0.01 0 0])))

                                    (get (:keys-state app) 87) ;; up - w
                                    (update :pos #(v+ % (v* time-delta [0 0.01 0])))

                                    (get (:keys-state app) 83) ;; down - s
                                    (update :pos #(v+ % (v* time-delta [0 -0.01 0]))))))
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
                                (assoc p :rotation [0 (/ time -8000) 0])))
             (object! :planet :pos [3 0 -5]
                      :radius 0.5
                      :material (material :image "images/burning-planet.jpg")
                      :update (fn [p time app time-delta]
                                (assoc p
                                       :pos [(* 3 (Math/sin (/ time 10000)))
                                             0
                                             (* 3 (Math/cos (/ time 10000)))])))]})
