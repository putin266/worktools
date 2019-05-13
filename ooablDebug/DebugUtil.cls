class com.qad.service.DebugUtil:
   method public void DispRootCause(rc as Progress.Lang.Error, level as integer):
      define variable i as integer no-undo.
      define variable e as com.qad.lang.AppError no-undo.
      define variable subrc as Progress.Lang.Error no-undo.
      if (type-of(rc, com.qad.lang.AppError)) then do:
         e = cast(rc, "com.qad.lang.AppError").
         do i = 1 to e:GetMessages():Count:
            message "AppError: level" string(level) " text:" cast(e:GetMessages():Get(i), "com.qad.lang.Message"):String.
         end.
         subrc = e:RootCause.
         if valid-object(subrc) then this-object:DispRootCause(subrc, level + 1).
      end.
   end.
end class.
