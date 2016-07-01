(ns planetslayer.core
  (:require-macros [cljs.core.async.macros :as a])
  (:require [cljs.core.async :as a]
            [rum.core :as r]
            [goog.dom :as dom]
            [planetslayer.anim :refer [request-animation-frame]]
            [planetslayer.scene :refer [make-scene-layer]]
            [planetslayer.renderer :refer [get-window-size renderer-component init-threejs! updater resizer]]
            [planetslayer.universes.planetslayer :as planetslayer]
            [planetslayer.universes.jumpout :as jumpout]
            [planetslayer.tests :refer [testbox]]))

(enable-console-print!)

(r/defc header [{:keys [version frames fps]}]
  [:div.header "Planet SLayer " version
   [:span.pull-right "Frames " frames " FPS " (subs (str fps) 0 5)]])

(r/defc footer [{:keys [version]}]
  [:div.footer [:.pull-left "github.com/tristanstraub/planetslayer"] "@www.allthethings.io"])

(defn get-pressed [keystate]
  {:e     (get keystate 69)
   :q     (get keystate 81)
   :s     (get keystate 83)
   :w     (get keystate 87)
   :d     (get keystate 68)
   :a     (get keystate 65)
   :shift (get keystate 16)
   :ctrl  (get keystate 17)})

(defn- yes-no [pressed]
  (if pressed "yes" "no"))

(r/defc controls [keystate controller]
  (let [pressed (get-pressed keystate)]
    [:div.controls
     [:div.row
      [:div.col-xs-3 [:span.label (if (:shift pressed) {:class "label-primary"}) "SHIFT"]]
      [:div.col-xs-3 [:span.label (if (:q pressed) {:class "label-primary"}) "Q"]]
      [:div.col-xs-3 [:span.label (if (:w pressed) {:class "label-primary"}) "W"]]
      [:div.col-xs-3 [:span.label (if (:e pressed) {:class "label-primary"}) "E"]]]
     [:div.row
      [:div.col-xs-3 [:span.label (if (:ctrl pressed) {:class "label-primary"}) "CTRL"]]
      [:div.col-xs-3 [:span.label (if (:a pressed) {:class "label-primary"}) "A"]]
      [:div.col-xs-3 [:span.label (if (:s pressed) {:class "label-primary"}) "S"]]
      [:div.col-xs-3 [:span.label (if (:d pressed) {:class "label-primary"}) "D"]]]
     [:div.row
      [:div.col-xs-4 [:span.label "Left joystick - horizontal"]]
      [:div.col-xs-4 (-> controller :left-joystick :horizontal)]]
     [:div.row
      [:div.col-xs-4 [:span.label "Left joystick - vertical"]]
      [:div.col-xs-4 (-> controller :left-joystick :vertical)]]
     (for [[i button] (map-indexed vector (-> controller :buttons))]
       [:div.row {:key i}
        [:div.col-xs-3 [:span.label i]]
        [:div.col-xs-3 (yes-no (:pressed button))]])]))

(r/defc platformer-todos [app]
  [:div.todos
   [:h3 "TODO"]
   [:ul
    [:li "Background layer"]
    [:li "Textures?"]
    [:li "Elevators! "]]])

(r/defc spacetrader-todos [app]
  [:div.todos
   [:h5 "On hold..."
    [:div [:h6 "planetslayer - spacetrader"]
     [:ul
      [:li.label-primary "Pivoting to platformer for variety"]
      [:li "SHIP building"
       [:ul ["Toolbar - part selection"]]
       [:ul [:li "DONE - GAMEPAD integration"]]]
      [:li "Planet landings"]]]]])

(r/defc root [app]
  (let [app @app]
    [:div
     (controls (:keystate app) (:controller app))
     (platformer-todos)
     (testbox)
     (header app)
     [:div.canvas-container
      (if (:webgl app)
        (renderer-component :webgl (:webgl app)))]
     (footer app)]))

;; (r/defc root [app]
;;   (let [app @app]
;;     [:div
;;      (controls (:keystate app) (:controller app))
;;      (platformer-todos)
;;      (header app)
;;      [:div.canvas-container
;;       (if (:webgl app)
;;         (renderer-component :webgl (:webgl app)))]
;;      (footer app)]))

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
  (-> app
      (update-in [:universe :objects]
                 (fn [objects]
                   (mapv (partial update-object (assoc app :keys-state keys-state)) objects)))
      (update-in [:universe :toolbar]
                 (fn [objects]
                   (mapv (partial update-object (assoc app :keys-state keys-state)) objects)))      ))


(defn clamp-difference [dst src max]
  (let [diff (- dst src)]
    (if (> diff max)
      (+ src diff)
      dst)))

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
  (let [!keys-state (atom {})
        debug false]
    {:!keys-state !keys-state
     :key-down!
     (fn [e]
       (swap! !keys-state assoc (.-keyCode e) true)
       (when debug
         (println (.-keyCode e) )))
     :key-up!
     (fn [e]
       (swap! !keys-state assoc (.-keyCode e) false)
       (when debug
         (println (.-keyCode e) ))
       )}))

(defn gamepad-button->clj [button]
  {:pressed (.-pressed button)
   :value (.-value button)})

(defn gamepad-buttons [gamepad]
  (let [buttons (.. gamepad -buttons)]
    (->> (range (.-length buttons))
         (mapv (fn [i] (gamepad-button->clj (aget buttons i)))))))

(defn update-controller! [app]
  (fn [time]
    (let [controller (first (:controllers @app))]
      (let [gamepad (if controller (aget (.. js/navigator (getGamepads)) (:index controller))
                        ;; TODO fix this hack, when the controller hasn't been reconnected
                        (aget (.. js/navigator (getGamepads)) 1))]
        (when gamepad
          (swap! app assoc :controller {:buttons       (gamepad-buttons gamepad)
                                        :left-joystick {:horizontal (aget (.. gamepad -axes) 0)
                                                        :vertical (aget (.. gamepad -axes) 1)}}))))))

(defn main []
  (when-let [stop! (:stop! @app)]
    (stop!))

  (let [{:keys [!keys-state key-down! key-up!]} (key-listener)
        !app                                    app
        gamepad-events                          (a/chan)
        stop-controller!                        (request-animation-frame (update-controller! !app))
        stop-universe-update!                   (request-animation-frame (timed-app-updater !app !keys-state))

        ;; stop-universe-update!                   (fn [])
        webgl                                   (init-threejs!)
        universe                                (jumpout/make-universe)
        ;;---
        scene-chan                              (make-scene-layer (get-window-size) (:objects universe))
        toolbar-chan                            (make-scene-layer (get-window-size) (:toolbar universe))
        gamepad-connected!                      (fn [e]
                                                  (.log js/console e)
                                                  (swap! app update :controllers
                                                         #(conj %
                                                                {:index (.. e -gamepad -index)
                                                                 :id (.. e -gamepad -id)
                                                                 :buttons (.. e -gamepad -buttons -length)
                                                                 :axes (.. e -gamepad -axes -length)})))]

    (a/go (let [scene           (a/<! scene-chan)
                toolbar-scene   (a/<! toolbar-chan)

                !universe       (atom universe)

                render!         #(.render webgl (:scene scene) (:camera scene))
                render-toolbar! #(.render webgl (:scene toolbar-scene) (:camera toolbar-scene))
                resize!         (resizer webgl (:camera scene) render!)
                update!         (updater #(:objects @!universe) scene)
                update-toolbar! (updater #(:toolbar @!universe) toolbar-scene)]

            (.. js/window (addEventListener "keydown" key-down! false))
            (.. js/window (addEventListener "keyup" key-up! false))
            (.. js/window (addEventListener "resize" resize! false))
            (.. js/window (addEventListener "gamepadconnected" gamepad-connected! false))

            (resize!)

            (let [stop! (request-animation-frame (fn [time]
                                                   (reset! !universe (:universe @app))
                                                   (update! time)
                                                   (update-toolbar! time)

                                                   (render!)
                                                   (render-toolbar!)))]

              (swap! app assoc
                     :version "0.0.1"
                     :universe universe
                     :webgl webgl
                     :stop! #(do
                               (stop-controller!)
                               (stop-universe-update!)
                               (stop!)
                               (.. js/window (removeEventListener "keydown" key-down! false))
                               (.. js/window (removeEventListener "keyup" key-up! false))
                               (.. js/window (removeEventListener "resize" resize!))
                               (.. js/window (removeEventListener "gamepadconnected" gamepad-connected!)))))))

    (let [rcomponent (r/mount (root app) (dom/getElement "root"))]
      (add-watch !keys-state :keys-changes (fn [k r o n]
                                             (swap! app assoc :keystate n)))
      (add-watch app :state-change (fn [k r o n]
                                     (when-not (= o n)
                                       (r/request-render rcomponent)))))))
