(define-library (llambda typed)
	(import (llambda internal primitives))
  (import (llambda nfi))

  ; Re-export from (llambda primitives) 
  (export define-type cast ann : define: define-record-type: lambda: make-predicate U Rec)

  ; Mutable pairs make handling (Pairof) and (Listof) very complex
  ; For example, consider the following code:
  ; (define: typed-pair : (Pairof <symbol> <exact-integer>) '(foo . 5))
  ; (define: untyped-pair : <pair> typed-pair)
  ; (set-car! untyped-pair #f)
  ; 
  ; This would violate the type constraint of "typed-pair" without directly modifying its value
  (cond-expand (immutable-pairs
    (export Pairof Listof List)))

  ; Export our type names
  (export <any> <list-element> <pair> <empty-list> <string> <symbol> <boolean> <number> <exact-integer> <flonum> <char>
          <vector> <bytevector> <procedure> <port>)

  ; These are new macros
  (export define-predicate let: let*: letrec*: letrec:)
  
  (begin
    (define-type <pair> (Pairof <any> <any>)))

  (begin 
    (define-syntax define-predicate
      (syntax-rules ()
                    ((define-predicate name type)
                     (define name (make-predicate type)))))

    (define-syntax let:
      (syntax-rules (:)
                    ((let: ((name : type val) ...) body1 body2 ...)
                     ((lambda: ((name : type) ...) body1 body2 ...)
                      val ...))))

    (define-syntax let*:
      (syntax-rules (:)
                    ((let*: () body1 body2 ...)
                     (let: () body1 body2 ...))
                    ((let*: ((name1 : type1 val1) (name2 : type2 val2) ...)
                       body1 body2 ...)
                     (let: ((name1 : type1 val1))
                       (let*: ((name2 : type2 val2) ...)
                         body1 body2 ...)))))

    (define-syntax letrec*:
      (syntax-rules (:)
                    ((letrec*: ((name : type val) ...) body1 body2 ...)
                     ((lambda ()
                        (define: name : type val) ...
                        body1 body2 ...)))))

    (define-syntax letrec:
      (syntax-rules (:)
                    ((letrec: ((name : type val) ...) body1 body2 ...)
                     (letrec*: ((name : type val) ...) body1 body2 ...)))))
)
