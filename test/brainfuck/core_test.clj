(ns brainfuck.core-test
  (:require [clojure.test :refer :all]
            [brainfuck.core :refer :all]))

(deftest empty-program
    (is (= (brainfuck-interpreter "") [0])))

(deftest increments-only
	(is (= (brainfuck-interpreter "+++") [3]))
	(is (= (brainfuck-interpreter "--") [-2]))
	(is (= (brainfuck-interpreter "++-") [1])))

(deftest moving
	(is (= (brainfuck-interpreter "+>++") [1 2]))
	(is (= (brainfuck-interpreter "+>-<+") [2 -1]))
	(is (= (brainfuck-interpreter ">>+>>-") [0 0 1 0 -1])))

(deftest loops
	(is (= (brainfuck-interpreter "+[-]") [0]))
	(is (= (brainfuck-interpreter "+++>[-]<[->+<]") [0 3]))
	(is (= (brainfuck-interpreter ">+>+++[-<[-<+++++>]<++[->+<]>>]<") [0 187 0]))
	)
