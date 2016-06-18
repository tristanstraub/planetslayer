(ns planetslayer.renderer
  (:require [rum.core :as r]
            [planetslayer.scene :refer [make-scene]]))

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

(def threejs
  {:did-mount (fn [state]
                (let [{:keys [universe]} (:rum/args state)
                      webgl              (init-threejs!)
                      scene              (make-scene (get-window-size) universe)
                      render!            #(.render webgl (:scene scene) (:camera scene))
                      resize!            (resizer webgl (:camera scene) render!)
                      ]

                  ;; TODO not the correct dom element
                  (.. js/document -body (appendChild (.-domElement webgl)))
                  (.. js/window (addEventListener "resize" resize! false))

                  (resize!)
                  (render!)

                  (assoc state ::resize resize!)))

   :transfer-state (fn [o n]
                     (assoc n ::resize (::resize o)))

   :will-unmount (fn [state]
                   (.. js/window (removeEventListener "resize" (::resize state)))
                   state)})

(r/defc renderer-component < threejs [& {:keys [universe]}]
  [:div])
