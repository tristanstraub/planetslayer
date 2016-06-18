#!/usr/bin/env boot
(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [rum "0.9.0"]
                 [pandeiro/boot-http "0.7.3"]
                 [adzerk/boot-cljs "1.7.228-1"]
                 [adzerk/boot-reload "0.4.7"]
                 [sablono "0.7.1"]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [enlive "1.1.6"]
                 [org.clojure/data.json "0.2.6"]])

(require '[pandeiro.boot-http :refer [serve]])
(require '[adzerk.boot-cljs :refer [cljs]])
(require '[adzerk.boot-reload :refer [reload]])

(deftask client []
  (comp (serve)
        (watch)
        (reload :on-jsload 'planetslayer.core/main)
        (cljs)))
