(ns planetslayer.anim)

(defn request-animation-frame
  ([stop f]
   (when-not @stop
     (js/requestAnimationFrame (fn [timestamp] (f timestamp) (request-animation-frame stop f)))))
  ([f]
   (let [stop (atom nil)]
     (request-animation-frame stop f)
     (fn [] (reset! stop true)))))
