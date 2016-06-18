(ns planetslayer.universe)

(defn planet [& {:keys [at material]}]
  {:type     :planet
   :pos      at
   :material material})

(defn material [& {:keys [color]}]
  {:color color})

(defn material-color [m]
  (:color m))

(defn make-universe []
  {:objects [(planet :at [0 0 0])
             (planet :at [3 0 -5]
                     :material (material :color 0x0000ff))]})
