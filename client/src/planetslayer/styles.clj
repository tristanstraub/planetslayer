(ns planetslayer.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]
            [garden.core :refer [css]]
            [garden.units :refer [px]]
            [garden.color :refer [rgb]]))

(def box {:border "1px solid white"
          :background "#777"
          :color "#fff"
          :position "absolute"
          :top "30px"
          :left "10px"
          :padding-right "12px"
          :padding-bottom "2px"
          :padding-left "2px"})

(defstyles style
  [:.canvas-container {:height "100%"}]

  [:.tests (merge box {
                       :top (px (+ 280 250))
                       :width (px 500)
                       :height (px 600)
                       })
   [:.test {:margin-bottom (px 20)
            :padding-bottom (px 5)
            :border-bottom "2px solid black"}]
   [:.failed {:background "red"
             :color "black"
              :margin-top (px 2)}]

   [:.success {:background "lightgreen"
             :color "black"
             :margin-top (px 2)}]]

  [:.footer {
             :position "absolute"
             :bottom "0px"
             :width "100%"
             :background "#777"
             :color "#fff"
             :border-top "1px solid #000"
             :text-align "right"
             :padding-right "10px"
             }]
  [:.header {
             :position "absolute"
             :top "0px"
             :padding-left "10px"
             :width "100%"
             :background "#777"
             :color "#fff"
             :border-bottom "1px solid #000"
             }]
  [:.controls {
               :overflow "scroll"
               :width "400px"
               :height (px 500)
               :border "1px solid white"
               :background "#777"
               :color "#fff"
               :position "absolute"
               :top "30px"
               :left "10px"
               :padding-right "12px"
               :padding-bottom "2px"
               :padding-left "2px"

               }]
  [:.todos {
            :bottom (px 30)
            :right (px 30)
            :z-index 10

            :width (px 400)
            :border "1px solid white"
            :background "#777"
            :color "#fff"
            :position "absolute"
            :padding-right "12px"
            :padding-bottom "2px"
            :padding-left "2px"
            }]

  )
