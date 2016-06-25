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

(defn threejs-move-to! [mesh pos]
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

    (threejs-move-to! mesh pos)

    (.add scene mesh)

    mesh))

(defn object-move-to! [mesh-index object pos]
  (threejs-move-to! (get mesh-index (:id object)) pos))

(defn object-rotate-to! [mesh-index object pos]
  (mesh-rotate-to! (get mesh-index (:id object)) pos))

(defn traverse-objects [layer scene & {:keys [load-texture load-model]}]
  (->> layer
       (reduce (fn [mesh-index object]
                 (let [texture (js/THREE.Texture.)
                       image   (-> object :material :image)
                       model   (-> object :model)]

                   (when image
                     (load-texture [texture image]))

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
                           (load-model [object model texture])
                           mesh-index)

                         :else
                         mesh-index)))
               {}))
  )

(defn load-textures [imgloader unloaded-textures done! resource-done!]
  (a/go (loop []
          (when-let [item (a/<! unloaded-textures)]
            (let [[texture image-name] item]
              (.load imgloader image-name
                     (fn [image]
                       (set! (.. texture -image) image)
                       (set! (.. texture -needsUpdate) true)
                       (resource-done!))))
            (recur)))
        (done!)))

(defn load-models [scene mesh-index jsonloader stlloader unloaded-models done! resource-done!]
  (a/go (loop []
          (when-let [item (a/<! unloaded-models)]
            (let [[object model texture] item]
              (cond (re-find #"[.]stl" model)
                    (do
                      (println :load-stl model)
                      (.load stlloader model
                             (fn [geo]
                               (.computeFaceNormals geo)
                               (.computeVertexNormals geo)

                               (let [mat (js/THREE.MeshPhongMaterial. #js {:color 0xffffff :map texture ;;  :side js/THREE.DoubleSide
                                                                           })
                                     mesh (js/THREE.Mesh. geo mat)]
                                 (.add scene mesh)

                                 (swap! mesh-index assoc (:id object) mesh)

                                 (if-let [pos (:pos object)]
                                   (threejs-move-to! mesh pos))
                                 ;;

                                 (if-let [rotate (:rotate object)]
                                   (mesh-rotate-to! mesh rotate))

                                 (if-let [scale (:scale object)]
                                   (mesh-scale-to! mesh scale))

                                 (resource-done!)))))
                    (re-find #"[.]json" model)
                    (do
                      (println :load-json model)

                      (.load jsonloader model
                             (fn [mesh]
                               (.. mesh (traverse (fn [child]
                                                    (when (= (type child) js/THREE.Mesh)
                                                      (set! (.. child -material -side) js/THREE.DoubleSide)))))

                               (.add scene mesh)

                               (swap! mesh-index assoc (:id object) mesh)

                               (if-let [pos (:pos object)]
                                 (threejs-move-to! mesh pos))

                               (if-let [rotate (:rotate object)]
                                 (mesh-rotate-to! mesh rotate))

                               (if-let [scale (:scale object)]
                                 (mesh-scale-to! mesh scale))

                               (resource-done!))))))
            (recur))))
  (done!))

(defn make-scene-layer [window layer]
  (let [{:keys [width height]} window
        scene                  (js/THREE.Scene.)
        camera                 (js/THREE.PerspectiveCamera. 45 (/ width height) 0.1 1000)
        light                  (js/THREE.AmbientLight. 0x999999)
        dlight                 (js/THREE.DirectionalLight. 0xbbbbbb)

        ;; -- assets
        mgr                    (js/THREE.LoadingManager.)

        ;; -- models

        stlloader              (js/THREE.STLLoader. mgr)
        jsonloader             (js/THREE.ObjectLoader. mgr)

        ;;-- textures
        imgloader              (js/THREE.ImageLoader. mgr)

        unloaded-models        (a/chan)
        unloaded-textures      (a/chan)

        mesh-index             (atom nil)
        done                   (a/chan)
        resources              (a/chan)
        resource-done!         (fn [] (a/put! done true))
        start-async            (fn [f]
                                 (a/put! resources true)
                                 (f
                                  (fn [] (a/put! done true))
                                  resource-done!))
        out                    (a/chan)]

    (start-async
     (fn [done! resource-done!]
       (a/go (let [new-mesh-index (traverse-objects layer
                                                    scene
                                                    :resources resources
                                                    :load-texture (fn [arg]
                                                                    (a/put! resources true)
                                                                    (a/put! unloaded-textures arg))
                                                    :load-model (fn [arg]
                                                                  (a/put! resources true)
                                                                  (a/put! unloaded-models arg)))]
               (reset! mesh-index new-mesh-index))

             (a/close! resources)
             (a/close! unloaded-textures)
             (a/close! unloaded-models)
             (done!))))

    (start-async
     (partial load-textures imgloader unloaded-textures))

    (start-async
     (partial load-models scene mesh-index jsonloader stlloader unloaded-models))

    (a/go
      (loop []
        (when (a/<! resources)
          (a/<! done)
          (recur)))

      (threejs-move-to! camera [1 1 10])
      (camera-look-at camera [1 0 0])
      (threejs-move-to! dlight [-0.5 0 1])

      (add-light scene light)
      (add-light scene dlight)

      (a/put! out {:scene scene :camera camera :mesh-index @mesh-index}))

    out))
