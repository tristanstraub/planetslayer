(ns planetslayer.scene
  (:require [planetslayer.universe :refer [make-universe material-color]]))

(declare add-sphere)

(defn vec->threejs [v]
  (js/THREE.Vector3. (v 0) (v 1) (v 2)))

(defn camera-look-at [camera pos]
  (.lookAt camera (vec->threejs pos)))

(defn add-light [scene light]
  (.add scene light))

(defn object-move-to [object pos]
  (.. object -position (set (pos 0) (pos 1) (pos 2))))

(defn add-sphere [scene & {:keys [pos color]}]
  (let [mat  (js/THREE.MeshPhongMaterial. #js {:color (or color 0xff0000)})
        geo  (js/THREE.SphereGeometry. 1 20 20)
        mesh (js/THREE.Mesh. geo mat)]

    (object-move-to mesh pos)

    (.add scene mesh)))

(defn make-scene [window universe]
  (let [{:keys [width height]} window
        scene                  (js/THREE.Scene.)
        camera                 (js/THREE.PerspectiveCamera. 45 (/ width height)
                                                            0.1 2000)
        light                  (js/THREE.AmbientLight. 0x555555)
        dlight                 (js/THREE.DirectionalLight. 0x999999)]

    (object-move-to camera [0 0 10])
    (camera-look-at camera [0 0 0])
    (object-move-to dlight [-0.5 0 1])

    (doseq [planet (:objects universe)]
      (add-sphere scene :pos (:pos planet) :color (material-color (:material planet))))

    (add-light scene light)
    (add-light scene dlight)

    {:scene scene :camera camera}))
