(ns planetslayer.core
  (:require [rum.core :as r]
            [goog.dom :as dom]))

(r/defc header [{:keys [version]}]
  [:div.header "Planet SLayer " version])

(r/defc footer [{:keys [version]}]
  [:div.footer "@www.allthethings.io"])

(r/defc root [app]
  [:div
   (header @app)
   (footer @app)
   ])

(defn main []
  (defonce app (atom {}))
  (swap! app assoc :version "0.0.1")
  (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
    (add-watch app :state-change (fn [k r o n]
                                   (when-not (= o n)
                                     (r/request-render comp))))))
