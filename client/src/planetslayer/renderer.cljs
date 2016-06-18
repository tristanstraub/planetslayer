(ns planetslayer.renderer
  (:require [planetslayer.threejs :refer [webgl]]
            [rum.core :as r]))

(def threejs
  {:did-mount (fn [state]
                (.. js/document -body (appendChild (.-domElement webgl)))
                state)})

(r/defc renderer-component < threejs []
  [:div])
