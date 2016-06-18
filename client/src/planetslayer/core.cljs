(ns planetslayer.core
  (:require [rum.core :as r]
            [goog.dom :as dom]
            [planetslayer.anim :refer [request-animation-frame]]
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

(defn get-time [app]
  (let [{:keys [previous-timestamp timestamp]} app]
     (- timestamp previous-timestamp)))

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

(defn- update-universe [app]
  (update-in app [:universe :objects] #(mapv (partial update-object app) %)))

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
  (defonce timer
    (request-animation-frame timed-app-update))

  (swap! app assoc :version "0.0.1" :universe (make-universe))
  (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
    (add-watch app :state-change (fn [k r o n]
                                   (when-not (= o n)
                                     (r/request-render rcomponent))))))
