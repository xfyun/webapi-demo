using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace webgate_ws_csharp.Common
{
    public enum StreamStatus
    {
        STATUS_FIRST_FRAME = 0,
        STATUS_CONTINUE_FRAME = 1,
        STATUS_LAST_FRAME = 2
    }

    public enum ResultStatus
    {
        STATUS_WORKING = 1,
        STATUS_FINISH = 2
    }
}
