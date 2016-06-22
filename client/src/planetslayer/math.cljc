(ns planetslayer.math)

(defn v*v [a b]
  (reduce + (map * a b)))

;; (defn zero-matrix [m]
;;   (mapv (fn [_] (into [] (take m (repeat 0)))) (range m)))

(defn identity-matrix [m]
  (mapv (fn [j] (mapv (fn [i] (if (= i j) 1 0)) (range m))) (range m)))

(defn matrix-transpose [m]
  (let [n (count (first m))]
    (mapv (fn [j]
            (mapv (fn [i] (get-in m [i j]))
                  (range n)))
          (range n))))

(defn m*m [a b]
  (let [n (count (first a))
        tb (matrix-transpose b)]
    (mapv (fn [i]
            (mapv (fn [j]
                    (v*v (nth a i)
                             (nth tb j)))
                  (range n)))
          (range n))))

(defn rotation-matrix [[x y z]]
  (let [cx (Math/cos x) sx (Math/sin x)
        cy (Math/cos y) sy (Math/sin y)
        cz (Math/cos z) sz (Math/sin z)
        Ax [[1 0 0]
            [0 cx sx]
            [0 (- sx) cx]]
        Ay [[cy 0 (- sy)]
            [0 1 0]
            [sy 0 cy]]
        Az [[cz sz 0]
            [(- sz) cz 0]
            [0 0 1]]]
    (m*m (m*m Az Ay) Ax)))

(defn m*v [a v]
  (let [n (count (first a))]
    (mapv (fn [vi] (v*v vi v))
          a)))

;; (defn smooth-x [x]
;;   (if (< (Math/abs x 0.0000001))
;;     0
;;     x))

;; (defn smooth [v]
;;   (mapv smooth-x v))
;; (defn translate-matrix [m v]
;;   (mapv (fn [j] (mapv (fn [i] (cond (= i j) 1
;;                                     (= i (- m 1)) (nth v j)
;;                                     :else 0))
;;                       (range m))) (range m)))

;; (defn scale-matrix [m s]
;;   (mapv (fn [j] (mapv (fn [i] (cond (= i j (- m 1)) 1
;;                                     (= i j) s
;;                                     :else 0))
;;                       (range m))) (range m)))


;; (defn frustrum [left right bottom top znear zfar]
;;   (let [x (/ (* 2 znear)
;;              (- right left))
;;         y (/ (* 2 znear)
;;              (- top bottom))
;;         a (/ (+ right left)
;;              (- right left))
;;         b (/ (+ top bottom)
;;              (- top bottom))
;;         c (/ (- (+ zfar znear))
;;              (- zfar znear))
;;         d (/ (* -2 zfar znear)
;;              (- zfar znear))]
;;     [[x 0 a 0]
;;      [0 y b 0]
;;      [0 0 c d]
;;      [0 0 -1 0]]))

;; (defn perspective [fovy aspect znear zfar]
;;   (let [ymax (* znear (Math/tan (* fovy (/ Math/PI 360.0))))
;;         ymin (- ymax)
;;         xmin (* ymin aspect)
;;         xmax (* ymax aspect)]
;;     (frustrum xmin xmax ymin ymax znear zfar)))


;; (defn flatten-matrix [m]
;;   (flatten (matrix-transpose m)))

;; (defn without-column [a i]
;;   (mapv (fn [row] (concat (take i row)
;;                           (drop (+ i 1) row)))
;;         a))

;; (defn without-row [a i]
;;   (concat (take i a)
;;           (drop (+ i 1) a)))

;; (defn minor-matrix [a i]
;;   (-> a
;;       (without-column i)
;;       (without-row 0)))

;; (defn width [a]
;;   (count (nth a 0)))

;; (declare determinant)

;; (defn cofactor [a i]
;;   (determinant (minor-matrix a i)))

;; (defn cofactor-term [i]
;;   (Math/pow -1 i))

;; (defn determinant [a]
;;   (cond (= (width a) 1) (nth (nth a 0) 0)
;;         :else (reduce sum (map (fn [i] (mul (cofactor-term (+ 1 i))
;;                                             (mul (nth (nth a 0) i)
;;                                                  (cofactor a i))))
;;                                (range (width a))))))

;; (defn cross-product [[i j k] [a b c] [d e f]]
;;   (determinant [[i j k]
;;                 [a b c]
;;                 [d e f]]))

;; (defn vecxy [x y]
;;   [x y])

;; (defn vec-diff [p1 p2]
;;   (apply vecxy (map - p1 p2)))

;; (defn sqrt [a]
;;   (Math/sqrt a))

;; (defn vec-dot [p1 p2]
;;   (reduce + (map * p1 p2)))

;; (defn vec-mul [v s]
;;   (apply vecxy (map (partial * s) v)))

;; (defn vec-magnitude [p1]
;;   (sqrt (vec-dot p1 p1)))

;; (defn vec-add [p1 p2]
;;   (apply vecxy (map + p1 p2)))

;; (defn deg->rad [degrees]
;;   (mod (* Math/PI (/ degrees 180)) (* 2 Math/PI)))

;; (defn rad->deg [radians]
;;   (mod (* 180 (/ radians Math/PI)) 360))

;; (defn vec-angle [p]
;;   (rad->deg (Math/atan2 (p 1) (p 0))))

;; (defn abs [value]
;;   (Math/abs value))

;; (defn normalize-angle [angle]
;;   (mod (+ 360 angle) 360))

;; (defn angle-diff [a b]
;;   (let [angle (- a b)]
;;     angle))

;; (defn angle-to [v1 v2]
;;   (vec-angle (vec-diff v2 v1)))

;; (defn asin [a]
;;   (Math/asin a))

;; (defn vec-normalize [a]
;;   (vec-mul a (/ 1 (vec-magnitude a))))

;; (defn angle-between [v1 v2]
;;   (rad->deg (asin (vec-dot (vec-normalize v1) (vec-normalize v2)))))

;; (defn min-angle [angle]
;;   (let [angle (normalize-angle angle)]
;;     (if (>= (abs angle) 180)
;;       (- angle 360)
;;       angle)))

;; (defn min-angle-diff [a b]
;;   (let [angle (normalize-angle (- (normalize-angle a) (normalize-angle b)))]
;;     (min-angle angle)))

;; (defn min-angle-to [v1 v2]
;;   (min-angle (angle-to v1 v2)))

;; (defn abs-angle-between [a b]
;;   (abs (min-angle-diff a b)))

;; (defn facing?
;;   ([angle p1 p2]
;;      (facing? angle p1 p2 1))
;;   ([angle p1 p2 error]
;;      (<= (abs-angle-between (angle-to p1 p2) angle)
;;          error)))

;; (defn sign [a]
;;   (if (< 0 a)
;;     1
;;     -1))

;; (defn sin [a]
;;   (Math/sin a))

;; (defn cos [a]
;;   (Math/cos a))

;; (defn intersects-disc? [[center radius] [v1 v-delta]]
;;   (let [ac (vec-diff center v1)
;;         theta (deg->rad (angle-between v-delta ac))]
;;     (<= (abs (* (vec-magnitude ac) (sin theta)))
;;         radius)))
