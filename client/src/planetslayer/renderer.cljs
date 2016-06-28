(ns planetslayer.renderer
  (:require [rum.core :as r]
            [planetslayer.anim :refer [request-animation-frame]]
            [planetslayer.scene :as s :refer [object-move-to! object-rotate-to! object-scale-to!]]
            [planetslayer.math :refer [v+v]]
            [planetslayer.universe :as u]))

(defn init-threejs! []
  (defonce *webgl* (js/THREE.WebGLRenderer.))
  (.setClearColor *webgl* 0x000000 0xff)
  (set! (.. *webgl* -autoClear) false)
  (.setPixelRatio *webgl* (.-devicePixelRatio js/window))
  *webgl*)

(defn get-window-size []
  {:width  (.-innerWidth js/window)
   :height (.-innerHeight js/window)})

(defn resize-camera! [camera]
  (let [{:keys [width height]} (get-window-size)]
    (case (.-type camera)
      "PerspectiveCamera"
      (do (set! (.. camera -aspect) (/ width height))
          (.updateProjectionMatrix camera))

      (do (set! (.. camera -left) -1)
          (set! (.. camera -right) 1)
          (set! (.. camera -top) (/ height width))
          (set! (.. camera -bottom) (* (/ height width) -1))
          (.updateProjectionMatrix camera)))))

(defn resize-webgl! [webgl]
  (let [{:keys [width height]} (get-window-size)]
    (.setSize webgl width height)))

(defn resizer [webgl camera render!]
  (fn []
    (resize-camera! camera)
    (resize-webgl! webgl)
    (render!)))

;; TODO move this to renderer?
(defn updater [layerf {:keys [scene camera mesh-index]}]
  (fn [time]
    (doseq [object (layerf)]
      (when-let [pos (:pos object)]
        (when (get mesh-index (:id object)) (object-move-to! mesh-index object pos)))

      (when-let [rot (:rotate object)]
        (when (not= :camera (:type object))
          (object-rotate-to! mesh-index object rot)))

      (when-let [scale (:scale object)]
        (object-scale-to! mesh-index object scale))

      (when-let [transparent (:transparent object)]
        ;;:transparent true :opacity 0.5

        (let [mesh (get mesh-index (:id object))]
          (.. mesh (traverse (fn [child]
                               (when (= (type child) js/THREE.Mesh)
                                 (set! (.. child -material -transparent) true)
                                 (set! (.. child -material -opacity) (or (:opacity object) 0.5))))))))


      (when-let [color (get-in object [:material :color])]
        (.. (get mesh-index (:id object)) -material -color (setHex color)))

      (when-let [pos (:pos object)]
        (let [pos-offset (or (:pos-offset object) [0 0 0])
              pos        (v+v pos pos-offset)
              mesh (get mesh-index (:id object))]
          (when mesh
            (s/threejs-move-to! mesh pos))))

      (when (u/camera? object)
        (when-let [pos (:pos object)]
          (let [pos-offset (or (:pos-offset object) [0 0 0])
                pos        (v+v pos pos-offset)]
            (s/threejs-move-to! camera pos)))

        (when-let [look-at (:look-at object)]
          (s/camera-look-at camera look-at))))))

(def threejs
  {:did-mount (fn [state]
                (let [c                        (:rum/react-component state)
                      el                       (js/ReactDOM.findDOMNode c)
                      {:keys [webgl]} (:rum/args state)]
                  (.. el (appendChild (.-domElement webgl))))
                state)})

(r/defc renderer-component < threejs [& {:keys [webgl]}]
  [:div])
