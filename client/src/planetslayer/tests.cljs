(ns planetslayer.tests
  (:require [rum.core :as r]
            [planetslayer.universes.jumpout :as j]))

(defn- yes-no [pressed]
  (if pressed "YES" "NO"))

(def tests
  {"intersects-with"
   (fn [assert!]
     (let [a {:pos [0 0 0] :scale [1 1 0]}]
       (assert! "intersects-with self"
                (j/intersects-with a a)))

     (let [a {:pos [0 0 0] :scale [1 1 0]}
           b {:pos [2 0 0] :scale [1 1 0]}]
       (assert! "does not intersect with a left of b"
                (not (j/intersects-with a b))))

     (let [a {:pos [0 2 0] :scale [1 1 0]}
           b {:pos [0 0 0] :scale [1 1 0]}]
       (assert! "does not intersect with a above b"
                (not (j/intersects-with a b))))

     (let [a {:pos [0 0.5 0] :scale [1 1 0]}
           b {:pos [0 0 0] :scale [1 1 0]}]
       (assert! "intersects with a over top of b"
                (j/intersects-with a b)))

     (let [a {:pos [0 0 0] :scale [1 1 0]}
           b {:pos [0.5 0 0] :scale [1 1 0]}]
       (assert! "intersects with a over left of b"
                (j/intersects-with a b))))

   "After update, player cell position is vector of integers"
   (fn [assert!]
     (let [p (j/player :pos [1.1 0.2 1])]
       (assert! "Cell position is incorrect"
                (= (:cell p) [1 0]) )
       (assert! "Pos is not set to initial value"
                (= (:pos p) [1.1 0.2 1]) )))

   "Player cell is initially positioned"
   (fn [assert!]
     (assert! "Player cell at top"
              (= [0 0] (j/player-cell
                        [["p"]
                         [" "]
                         ["x"]])))
     (assert! "Player cell in the middle" (= [1 0] (j/player-cell
                                                    [[" "]
                                                     ["p"]
                                                     ["x"]]))))
   ;; "Player falls onto floor"
   ;; (fn [assert!]
   ;;   (assert! "Player colliding with floor" (= [1 0] (j/update-world
   ;;                                                    [["p"]
   ;;                                                     [" "]
   ;;                                                     ["x"]]))))

   ;; "Player position should be a vector of integers2"
   ;; (fn [assert]
   ;;   (assert true "this thing is not true"))
   })

(defn run-tests [testmap]
  (->> testmap
       (map (fn [[description test!]]
              (let [assertions (atom [])
                    error      (fn [message] (swap! assertions conj [false message]))
                    success    (fn [message] (swap! assertions conj [true message]))
                    assert!    (fn [message val]
                                 (if val
                                   (success message)
                                   (error message)))]

                (test! assert!)

                {:description description
                 :success     (empty? (remove first @assertions))
                 :assertions @assertions})))))

(r/defc testbox []
  [:.tests [:h1 test]
   (for [{:keys [description success assertions]} (run-tests tests)]
     [:div.test
      [:div.row {:key description}
       [:div.col-xs-6 description]
       [:div.col-xs-6 {:class (if success "success" "failed")} (yes-no success)]]
      [:div.row
       (for [assertion assertions]
         [:div.col-xs-offset-3.col-xs-9 {:class (if (first assertion) "success" "failed")} (second assertion)])]])])
