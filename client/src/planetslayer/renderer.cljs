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
      (when-let [pos (:pos object)]
        (when (get mesh-index (:id object)) (object-move-to! mesh-index object pos)))
      (when-let [rot (:rotation object)]
        (object-rotate-to! mesh-index object rot)))))

(def threejs
  {:did-mount (fn [state]
                (let [c                        (:rum/react-component state)
                      el                       (js/ReactDOM.findDOMNode c)
                      {:keys [webgl]} (:rum/args state)]
                  (.. el (appendChild (.-domElement webgl))))
                state)})

(r/defc renderer-component < threejs [& {:keys [webgl]}]
  [:div])
