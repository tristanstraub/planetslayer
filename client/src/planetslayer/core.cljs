(ns planetslayer.core
  (:require [rum.core :as r]
            [goog.dom :as dom]
            [planetslayer.renderer :refer [renderer-component]]
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
     (renderer-component :universe (:universe app))
     (footer app)]))

;; App state -- globals? oh well...
(defonce app (atom {}))

(defn request-animation-frame [f]
  (js/requestAnimationFrame (fn [timestamp] (f timestamp) (request-animation-frame f))))

(defn get-fps [app]
  (let [{:keys [start-timestamp previous-timestamp timestamp]} app
        delta                                                  (- timestamp previous-timestamp)]
    (if previous-timestamp
      (/ 1000.0 delta)
      0)))

(defn update-fps [app]
  (assoc app :fps (get-fps app)))

(defn timer-update [timestamp]
  (swap! app (fn [app]
               (-> app
                   (update :start-timestamp #(or % timestamp))
                   (update :frames inc)
                   (assoc :previous-timestamp (:timestamp app))
                   (assoc :timestamp timestamp)
                   (update-fps)))))

(defonce timer
  (request-animation-frame timer-update))

(defn main []
  (swap! app assoc :version "0.0.1" :universe (make-universe))
  (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
    (add-watch app :state-change (fn [k r o n]
                                   (when-not (= o n)
                                     (r/request-render rcomponent))))))
