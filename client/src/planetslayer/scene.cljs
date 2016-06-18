(ns planetslayer.scene)

(declare add-sphere)

(defn vec->threejs [v]
  (js/THREE.Vector3. (v 0) (v 1) (v 2)))

(defn camera-move-to [camera pos]
  (.. camera -position (set (pos 0) (pos 1) (pos 2))))

(defn camera-look-at [camera pos]
  (.lookAt camera (vec->threejs pos)))

(defn add-light [scene light]
  (.add scene light))

(defn- light-move-to [light pos]
  (.. light -position (set (pos 0) (pos 1) (pos 2))))

(defn make-scene [window]
  (let [{:keys [width height]} window
        scene                  (js/THREE.Scene.)
        camera                 (js/THREE.PerspectiveCamera. 45 (/ width height)
                                                            0.1 2000)
        light                  (js/THREE.AmbientLight. 0x555555)
        dlight                 (js/THREE.DirectionalLight. 0xff0000)]

    (camera-move-to camera [0 0 10])
    (camera-look-at camera [0 0 0])
    (light-move-to dlight [-0.5 0 1])

    (add-sphere scene)
    (add-light scene light)
    (add-light scene dlight)

    {:scene scene :camera camera}))

(defn add-sphere [scene]
  (let [mat  (js/THREE.MeshPhongMaterial. #js {:color 0xff0000})
        geo  (js/THREE.SphereGeometry. 1 20 20)
        mesh (js/THREE.Mesh. geo mat)]

    (.add scene mesh)))
