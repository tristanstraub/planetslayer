(ns planetslayer.anim)

(defn clamp-difference [dst src max]
  (let [diff (- dst src)]
    (if (> diff max)
      (+ src max)
      dst)))

(defn request-animation-frame
  ([stop f previous-timestamp]
   (when-not @stop
     (js/requestAnimationFrame (fn [timestamp]
                                 (let [new-timestamp (clamp-difference timestamp previous-timestamp (/ 1000 240))]

                                   (f new-timestamp)

                                   (request-animation-frame stop f timestamp))))))
  ([f]
   (let [stop (atom nil)]
     (request-animation-frame stop f nil)
     (fn [] (reset! stop true)))))
