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
  [:div.footer [:.pull-left "github.com/tristanstraub/planetslayer"] "@www.allthethings.io"])

(defn get-pressed [keystate]
  {:e (get keystate 69)
   :q (get keystate 81)
   :s (get keystate 83)
   :w (get keystate 87)
   :d (get keystate 68)
   :a (get keystate 65)})

(r/defc controls [keystate]
  (let [pressed (get-pressed keystate)]
    [:div.controls
     [:div.row
      [:div.col-xs-4 [:span.label (if (:q pressed) {:class "label-primary"}) "Q"]]
      [:div.col-xs-4 [:span.label (if (:w pressed) {:class "label-primary"}) "W"]]
      [:div.col-xs-4 [:span.label (if (:e pressed) {:class "label-primary"}) "E"]]]
     [:div.row
      [:div.col-xs-4 [:span.label (if (:a pressed) {:class "label-primary"}) "A"]]
      [:div.col-xs-4 [:span.label (if (:s pressed) {:class "label-primary"}) "S"]]
      [:div.col-xs-4 [:span.label (if (:d pressed) {:class "label-primary"}) "D"]]]]))

(r/defc root [app]
  (let [app @app]
    [:div
     (controls (:keystate app))
     (header app)
     (if (:webgl app)
       (renderer-component :webgl (:webgl app)))
     (footer app)]))

;; App state -- globals? oh well...
(defonce app (atom {}))

(defn get-time [app]
  (let [{:keys [start-timestamp timestamp]} app]
    (- timestamp start-timestamp)))

(defn get-time-delta [app]
  (let [{:keys [timestamp previous-timestamp]} app]
    (if previous-timestamp
      (- timestamp previous-timestamp)
      (get-time app))))

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
    (up object (get-time app) app (get-time-delta app))
    object))

(defn update-universe [app keys-state]
  (update-in app [:universe :objects]
             (fn [objects]
               (mapv (partial update-object (assoc app :keys-state keys-state)) objects))))

(defn timed-app-updater [!app !keys-state]
  (fn [timestamp]
    (swap! !app (fn [app]
                 (-> app
                     (update :start-timestamp #(or % timestamp))
                     (update :frames inc)
                     (assoc :previous-timestamp (:timestamp app))
                     (assoc :timestamp timestamp)
                     (update-fps)
                     (update-universe @!keys-state))))))


(defn key-listener []
  (let [!keys-state (atom {})]
    {:!keys-state !keys-state
     :key-down!
     (fn [e]
       (swap! !keys-state assoc (.-keyCode e) true)
)
     :key-up!
     (fn [e]
       (swap! !keys-state assoc (.-keyCode e) false)
)}))

(defn main []
  (when-let [stop! (:stop! @app)]
    (stop!))

  (let [{:keys [!keys-state key-down! key-up!]} (key-listener)
        !app                                    app
        stop-universe-update!                   (request-animation-frame (timed-app-updater !app !keys-state))
        webgl                                   (init-threejs!)
        universe                                (make-universe)
        ;;---
        scene-chan                              (make-scene (get-window-size) universe)]

    (a/go (let [scene                       (a/<! scene-chan)
                !universe                   (atom universe)
                render!                     #(.render webgl (:scene scene) (:camera scene))
                resize!                     (resizer webgl (:camera scene) render!)
                update!                     (updater !universe scene)]

            (.. js/window (addEventListener "keydown" key-down! false))
            (.. js/window (addEventListener "keyup" key-up! false))
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
                               (stop-universe-update!)
                               (stop!)
                               (.. js/window (removeEventListener "keydown" key-down! false))
                               (.. js/window (removeEventListener "keyup" key-up! false))
                               (.. js/window (removeEventListener "resize" resize!)))))))

    (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
      (add-watch !keys-state :keys-changes (fn [k r o n]
                                             (swap! app assoc :keystate n)))
      (add-watch app :state-change (fn [k r o n]
                                     (when-not (= o n)
                                       (r/request-render rcomponent)))))))
