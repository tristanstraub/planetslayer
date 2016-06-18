(ns planetslayer.universe)

(defn planet! [& {:keys [at material update]}]
  (defonce ids (atom 0))
  {:id       (swap! ids inc)
   :type     :planet
   :pos      at
   :material material
   :update   update})

(defn material [& {:keys [color]}]
  {:color color})

(defn material-color [m]
  (:color m))

(defn make-universe []
  {:objects [(planet! :at [0 0 0])
             (planet! :at [3 0 -5]
                      :material (material :color 0x0000ff)
                      :update (fn [p time]
                                (assoc p :at [(* 3 (Math/sin (/ time 1000))) 0 -5])))]})
