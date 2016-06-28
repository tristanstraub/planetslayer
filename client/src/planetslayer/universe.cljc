(ns planetslayer.universe
  (:require [planetslayer.math :refer [m*v v+v v-norm v* ;; v^ v* v*v |v|
                                       identity-matrix rotation-matrix]]))

(defn id! []
  (defonce ids (atom 0))
  (swap! ids inc))

(defn object! [type & {:keys [tag attributes pos material update radius scale model rotate look-at transparent opacity camera-type]}]
  {:id          (id!)
   :type        type
   :attributes  attributes
   :camera-type camera-type
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
   :opacity     opacity
   :velocity    [0 0 0]})

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
  (->> universe
       (map second)
       (apply concat)))

(defn get-dir [player]
  (m*v (rotation-matrix (:rotate player)) [1 0 0]))

(defn threejs->vec [r]
  [(.-x r) (.-y r) (.-z r)])

(defn look-through-player [camera player]
  (let [v       (get-dir player)]
    (assoc camera :look-at (v+v (:pos player) v))))

(defn get-player [app]
  (first (filter (comp #(= % :player) :tag) (-> app :universe :objects))))

(defn layer-get-camera [layer]
  (->> layer
       (filter (comp #(= % :camera) :type))
       first))
