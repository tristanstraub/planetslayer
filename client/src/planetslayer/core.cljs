(ns planetslayer.core
  (:require-macros [cljs.core.async.macros :as a])
  (:require [cljs.core.async :as a]
            [rum.core :as r]
            [goog.dom :as dom]
            [planetslayer.anim :refer [request-animation-frame]]
            [planetslayer.scene :refer [make-scene]]
            [planetslayer.renderer :refer [get-window-size renderer-component init-threejs! updater resizer]]
            [planetslayer.universe :refer [make-universe]]))

(enable-console-print!)

(r/defc header [{:keys [version frames fps]}]
  [:div.header "Planet SLayer " version
   [:span.pull-right "Frames " frames " FPS " (subs (str fps) 0 5)]])

(r/defc footer [{:keys [version]}]
  [:div.footer "@www.allthethings.io"])

(r/defc root [app]
  (let [app @app]
    [:div
     (header app)
     (if (:webgl app)
       (renderer-component :webgl (:webgl app)))
     (footer app)]))

;; App state -- globals? oh well...
(defonce app (atom {}))

(defn get-time [app]
  (let [{:keys [start-timestamp timestamp]} app]
     (- timestamp start-timestamp)))

(defn get-fps [app]
  (let [{:keys [start-timestamp previous-timestamp timestamp]} app
        delta                                                  (- timestamp previous-timestamp)]
    (if previous-timestamp
      (/ 1000.0 delta)
      0)))

(defn update-fps [app]
  (assoc app :fps (get-fps app)))

(defn update-object [app object]
  (if-let [up (:update object)]
    (up object (get-time app))
    object))

(defn update-universe [app]
  (update-in app [:universe :objects]
             (fn [objects]
               (mapv (partial update-object app) objects))))

(defn timed-app-update [timestamp]
  (swap! app (fn [app]
               (-> app
                   (update :start-timestamp #(or % timestamp))
                   (update :frames inc)
                   (assoc :previous-timestamp (:timestamp app))
                   (assoc :timestamp timestamp)
                   (update-fps)
                   (update-universe)))))

(defn main []
  (when-let [stop! (:stop! @app)]
    (stop!))

  (defonce timer
    (request-animation-frame #'timed-app-update))

  (let [webgl      (init-threejs!)
        universe   (make-universe)
        ;;---
        scene-chan (make-scene (get-window-size) universe)]

    (a/go (let [scene     (a/<! scene-chan)
                !universe (atom universe)
                render!   #(.render webgl (:scene scene) (:camera scene))
                resize!   (resizer webgl (:camera scene) render!)
                update!   (updater !universe scene)]

            (.. js/window (addEventListener "resize" resize! false))

            (resize!)

            (let [stop! (request-animation-frame (fn [time]
                                                   (reset! !universe (:universe @app))
                                                   (update! time)
                                                   (render!)))]

              (swap! app assoc
                     :version "0.0.1"
                     :universe universe
                     :webgl webgl
                     :stop! #(do
                               (stop!)
                               (println :remove-resize)
                               (.. js/window (removeEventListener "resize" resize!))))))))

  (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
    (add-watch app :state-change (fn [k r o n]
                                   (when-not (= o n)
                                     (r/request-render rcomponent))))))
