(ns planetslayer.universe)

(defn id! []
  (defonce ids (atom 0))
  (swap! ids inc))

(defn planet! [& {:keys [at material update radius]}]
  {:id       (id!)
   :type     :planet
   :pos      at
   :material material
   :update   update
   :radius   radius})

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

(defn ship! [& {:keys [at material update radius]}]
  {:id       (id!)
   :type     :planet
   :pos      at
   :material material
   :update   update
   :radius   radius})

(defn make-universe []
  {:objects [(ship! :at [2 0 0])
             (planet! :at [0 0 0]
                         :radius 1
                         :material (material :image "images/sun.jpg"
                                             :color 0xaaaaaa)
                         :update (fn [p time]
                                   (assoc p :rotation [0 (/ time -8000) 0])))
             (planet! :at [3 0 -5]
                      :radius 0.5
                      :material (material :image "images/burning-planet.jpg")
                      :update (fn [p time]
                                (assoc p
                                       :at [(* 3 (Math/sin (/ time 10000)))
                                            0
                                            (* 3 (Math/cos (/ time 10000)))])))]})
