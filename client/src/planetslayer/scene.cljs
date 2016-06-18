(ns planetslayer.scene
  (:require [planetslayer.universe :refer [material-color material-image]]))

(defn vec->threejs [v]
  (js/THREE.Vector3. (v 0) (v 1) (v 2)))

(defn camera-look-at [camera pos]
  (.lookAt camera (vec->threejs pos)))

(defn add-light [scene light]
  (.add scene light))

(defn mesh-move-to! [mesh pos]
  (.. mesh -position (set (pos 0) (pos 1) (pos 2))))

(defn mesh-rotate-to! [mesh pos]
  (.. mesh -rotation (set (pos 0) (pos 1) (pos 2))))

(defn add-sphere [scene & {:keys [pos color radius texture]}]
  (let [mat    (js/THREE.MeshPhongMaterial. #js {:color (or color 0xff0000)
                                                 :map texture})
        geo    (js/THREE.SphereGeometry. radius 20 20)
        mesh   (js/THREE.Mesh. geo mat)]

    (mesh-move-to! mesh pos)

    (.add scene mesh)

    mesh))

(defn object-move-to! [mesh-index object pos]
  (mesh-move-to! (get mesh-index (:id object)) pos))

(defn object-rotate-to! [mesh-index object pos]
  (mesh-rotate-to! (get mesh-index (:id object)) pos))

(defn make-scene [window universe]
  (let [{:keys [width height]} window
        scene                  (js/THREE.Scene.)
        camera                 (js/THREE.PerspectiveCamera. 45 (/ width height) 0.1 2000)
        light                  (js/THREE.AmbientLight. 0x999999)
        dlight                 (js/THREE.DirectionalLight. 0xbbbbbb)

        ;;-- textures
        mgr                    (js/THREE.LoadingManager.)
        imgloader              (js/THREE.ImageLoader. mgr)


        mesh-index             (reduce (fn [mesh-index planet]
                                         (assoc mesh-index
                                                (:id planet)
                                                (add-sphere scene
                                                            :pos (:pos planet)
                                                            :color (material-color (:material planet))
                                                            :radius (or (:radius planet) 1)
                                                            :texture (js/THREE.Texture.))))
                                       {}
                                       (:objects universe))]

    (doseq [object (:objects universe)]
      (let [mesh (get mesh-index (:id object))
            texture (.. mesh -material -map)]
        (when-let [image (material-image (:material object))]
          (.load imgloader image
                 (fn [image]
                   (set! (.. texture -image) image)
                   (set! (.. texture -needsUpdate) true))))))

    (mesh-move-to! camera [0 0 10])
    (camera-look-at camera [0 0 0])
    (mesh-move-to! dlight [-0.5 0 1])

    (add-light scene light)
    (add-light scene dlight)

    {:scene scene :camera camera :mesh-index mesh-index}))
