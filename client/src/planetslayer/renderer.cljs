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

(defn resizer [webgl]
  (fn []
    (resize-webgl! webgl)))

(def threejs
  {:did-mount (fn [state]
                (let [webgl   (init-threejs!)
                      resize! (resizer webgl)
                      scene   (make-scene (get-window-size))]

                  (.. js/document -body (appendChild (.-domElement webgl)))
                  (.. js/window (addEventListener "resize" resize! false))

                  (resize!)
                  (.render webgl (:scene scene) (:camera scene))

                  (assoc state ::resize resize!)))

   :transfer-state (fn [o n]
                     (assoc n ::resize (::resize o)))

   :will-unmount (fn [state]
                   (.. js/window (removeEventListener "resize" (::resize state)))
                   state)})

(r/defc renderer-component < threejs []
  [:div])
