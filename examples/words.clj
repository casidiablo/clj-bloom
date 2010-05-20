(ns words
  (:require
   [ clojure.contrib.duck-streams   :as ds]
   [com.github.kyleburton.clj-bloom :as bf]))

(def *words-file* "/usr/share/dict/words")

(defn make-hash-fn-crc32 [#^String x]
  (let [crc (java.util.zip.CRC32.)]
    (fn [#^String s bytes]
      (.reset crc)
      (.update crc (.getBytes (.toLowerCase (str s x))))
      (mod (.getValue crc)
           bytes))))

(defn make-hash-fn-adler32 [#^String x]
  (let [crc (java.util.zip.Adler32.)]
    (fn [#^String s bytes]
      (.reset crc)
      (.update crc (.getBytes (.toLowerCase (str s x))))
      (mod (.getValue crc)
           bytes))))


(defn run [hash-fns]
  (let [filter (bf/make-bloom-filter (* 10 1024 1024) hash-fns
                                     )]
    (dorun
     (doseq [line (ds/read-lines *words-file*)]
       (bf/add! filter (.toLowerCase line))))
    (dorun
     (doseq [w (.split "The quick brown ornithopter hyper-jumped over the lazy trollusk" "\\s+")]
       (if (bf/include? filter (.toLowerCase w))
         (prn (format "HIT:  '%s' in the filter" w))
         (prn (format "MISS: '%s' not in the filter" w)))))))

;; CRC32:12s, hashCode:11s, Adler32:12s, md5:13s, sha1:14s
;;  (time (run))

(prn "fn:hashCode")
(time (run bf/*default-hash-fns*))
(prn "fn:adler32")
(time (run (map make-hash-fn-adler32 ["1" "2" "3" "4" "5"])))
(prn "fn:crc32")
(time (run (map make-hash-fn-crc32   ["1" "2" "3" "4" "5"])))
(prn "fn:md5")
(time (run (map bf/make-hash-fn-md5  ["1" "2" "3" "4" "5"])))
(prn "fn:sha1")
(time (run (map bf/make-hash-fn-sha1 ["1" "2" "3" "4" "5"])))
