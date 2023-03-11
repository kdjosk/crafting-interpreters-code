data Expr a =  Add (Expr a) (Expr a) | Sub (Expr a) (Expr a) | Literal a


stringify :: Show a => Expr a -> String
stringify (Add x y) = "( + " ++ stringify x ++ " " ++ stringify y ++ ")"
stringify (Sub x y) = "( - " ++ stringify x ++ " " ++ stringify y ++ ")"
stringify (Literal x) = show x
