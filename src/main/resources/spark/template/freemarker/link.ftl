<!DOCTYPE html>
<html>
<head>
    <script src="https://code.jquery.com/jquery-1.10.2.js"/>
    <script type='text/javascript'>
            setInterval(function () {
                $.ajax({
                    url: "gettv",
                    timeout: 2000
                }).done(function(data) {
                    console.log("HERE");
                    if (data.url) {
                        $(location).attr('href', '/');
                    } else {
                        console.log("No binding");
                    }
                });
            }, 3000);
    </script>
</head>

<body>

    <img src="${data}" id="qrcode" height="80%" align="middle"/>
    <h3>
        ${tvid}
    </h3>

</body>
</html>
