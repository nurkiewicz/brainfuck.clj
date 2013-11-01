(ns com.blogspot.nurkiewicz.brainfuck.compiler-test
  (:require [clojure.test :refer :all]
			[com.blogspot.nurkiewicz.brainfuck.compiler :refer :all]))

(deftest empty-program
	(is (= (brainfuck) {:ptr 0 :cells [0]})))

(deftest increments-only
	(is (= (brainfuck +++) {:ptr 0 :cells [3]}))
	(is (= (brainfuck --) {:ptr 0 :cells [-2]}))
	(is (= (brainfuck ++-) {:ptr 0 :cells [1]})))

(deftest moving
	(is (= (brainfuck +>++) {:ptr 1 :cells [1 2]}))
	(is (= (brainfuck +>-<+) {:ptr 0 :cells [2 -1]}))
	(is (= (brainfuck >>+>>-) {:ptr 4 :cells [0 0 1 0 -1]})))

(deftest loops
	(is (= (brainfuck +[-]>) {:ptr 1 :cells [0 0]}))
	(is (= (brainfuck +++>[-]<[->+<]) {:ptr 0 :cells [0 3]}))
	(is (= 
	(brainfuck 
		>+>+++[
		  -<[
			-<+++++>
		  ]
		  <++[
			->+<
		  ]
		>>
		]
	  <
	) 
	{:ptr 1 :cells [0 187 0]})))
