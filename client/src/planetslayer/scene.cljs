(ns planetslayer.scene
  (:require-macros [cljs.core.async.macros :as a])
  (:require [planetslayer.universe :as u :refer [material-color material-image planets planet?]]
            [cljs.core.async :as a]))

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

(defn mesh-scale-to! [mesh pos]
  (.. mesh -scale (set (pos 0) (pos 1) (pos 2))))

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

        ;; -- assets
        mgr                    (js/THREE.LoadingManager.)

        ;; -- models

        stlloader              (js/THREE.STLLoader. mgr)

        ;;-- textures
        imgloader              (js/THREE.ImageLoader. mgr)

        unloaded-models        (a/chan)
        unloaded-textures      (a/chan)

        mesh-index             (atom (reduce (fn [mesh-index object]
                                               (let [texture (js/THREE.Texture.)
                                                     image   (-> object :material :image)
                                                     model   (-> object :model)]

                                                 (when image
                                                   (a/put! unloaded-textures [texture image]))

                                                 (cond (planet? object)
                                                       (assoc mesh-index
                                                              (:id object)
                                                              (add-sphere scene
                                                                          :pos (:pos object)
                                                                          :color (material-color (:material object))
                                                                          :radius (or (:radius object) 1)
                                                                          :texture texture))

                                                       model
                                                       (do
                                                         (a/put! unloaded-models [object model])
                                                         mesh-index)

                                                       :else
                                                       mesh-index)))
                                             {}
                                             (u/objects universe)))]

    (a/close! unloaded-textures)
    (a/close! unloaded-models)

    (let [done (a/chan)
          out  (a/chan)]
      (a/go (loop []
              (when-let [item (a/<! unloaded-textures)]
                (let [[texture image] item]
                  (println :image image)
                  (.load imgloader image
                         (fn [image]
                           (set! (.. texture -image) image)
                           (set! (.. texture -needsUpdate) true))))
                (recur)))
            (a/put! done true))

      (a/go (loop []
              (when-let [item (a/<! unloaded-models)]
                (println :model? item)
                (let [[object model] item]
                  (.load stlloader model
                         (fn [geo]
                           (println :loaded-model model)
                           (.computeFaceNormals geo)
                           (.computeVertexNormals geo)
                           (let [mat (js/THREE.MeshPhongMaterial. #js {:color 0x000077})
                                 mesh (js/THREE.Mesh. geo mat)]
                             (.add scene mesh)
                             (swap! mesh-index assoc (:id object) mesh)


                             (if-let [pos (:pos object)]
                               (mesh-move-to! mesh pos))
                             ;;

                             (if-let [rotate (:rotate object)]
                               (mesh-rotate-to! mesh rotate))

                             (if-let [scale (:scale object)]
                               (mesh-scale-to! mesh scale))

                             ;; (.. object (traverse (fn [child]
                             ;;                        (when (= (type child) js/THREE.Mesh)
                             ;;                          (set! (.. child -material) mat)))))
                             )
                           )))
                (recur)))
            (a/put! done true))

      (a/go
        (println :wait)
        (doseq [_ (range 2)]
          (a/<! done))
        (println :ready)

        (mesh-move-to! camera [0 0 10])
        (camera-look-at camera [0 0 0])
        (mesh-move-to! dlight [-0.5 0 1])

        (add-light scene light)
        (add-light scene dlight)

        (a/put! out {:scene scene :camera camera :mesh-index @mesh-index}))

      out)))
