(ns planetslayer.renderer
  (:require [rum.core :as r]
            [planetslayer.anim :refer [request-animation-frame]]
            [planetslayer.scene :refer [make-scene object-move-to! object-rotate-to!]]))

(defn init-threejs! []
  (defonce *webgl* (js/THREE.WebGLRenderer.))
  (.setPixelRatio *webgl* (.-devicePixelRatio js/window))
  *webgl*)

(defn get-window-size []
  {:width  (.-innerWidth js/window)
   :height (.-innerHeight js/window)})

(defn resize-camera! [camera]
  (let [{:keys [width height]} (get-window-size)]
    (set! (.. camera -aspect) (/ width height))
    (.updateProjectionMatrix camera)))

(defn resize-webgl! [webgl]
  (let [{:keys [width height]} (get-window-size)]
    (.setSize webgl width height)))

(defn resizer [webgl camera render!]
  (fn []
    (resize-camera! camera)
    (resize-webgl! webgl)
    (render!)))

(defn updater [!universe {:keys [scene camera mesh-index]}]
  (fn [time]
    (doseq [object (:objects @!universe)]
      (when-let [pos (:at object)]
        (object-move-to! mesh-index object pos))
      (when-let [rot (:rotation object)]
        (object-rotate-to! mesh-index object rot)))))

(def threejs
  {:did-mount (fn [state]
                (let [{:keys [universe]} (:rum/args state)
                      !universe          (atom universe)
;;;---
                      webgl              (init-threejs!)
                      scene              (make-scene (get-window-size) universe)
                      render!            #(.render webgl (:scene scene) (:camera scene))
                      resize!            (resizer webgl (:camera scene) render!)
                      update!            (updater !universe scene)
                      ]

                  ;; TODO not the correct dom element
                  (.. js/document -body (appendChild (.-domElement webgl)))
                  (.. js/window (addEventListener "resize" resize! false))

                  (resize!)

                  (let [stop! (request-animation-frame (fn [time]
                                                         (update! time)
                                                         (render!)))]
                    (assoc state ::resize resize! ::stop! stop! ::!universe !universe))))

   :did-update (fn [state]
                 (let [{:keys [universe]} (:rum/args state)]
                   (reset! (::!universe state) universe)
                   state))

   :transfer-state (fn [o n]
                     (assoc n ::resize (::resize o) ::stop! (::stop! o) ::!universe (::!universe o)))

   :will-unmount (fn [state]
                   (.. js/window (removeEventListener "resize" (::resize state)))
                   ((::stop! state))
                   state)})

(r/defc renderer-component < threejs [& {:keys [universe]}]
  [:div])
