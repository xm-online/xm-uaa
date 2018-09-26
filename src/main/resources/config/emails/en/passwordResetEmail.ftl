<!DOCTYPE html>
<html>
    <head>
        <title>uaa password reset</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <link rel="shortcut icon" href="${baseUrl}/favicon.ico" />
    </head>
    <body>
        <p>
            Dear ${user.firstName}
        </p>
        <p>
            For your XM^online account a password reset was requested, please click on the URL below to reset it:
        </p>
        <p>
            <a href="${baseUrl}/reset/finish?key=${user.resetKey}">Reset Link</a>
        </p>
        <p>
            <span>Regards, </span>
            <br/>
            <em>XM^online Team.</em>
        </p>
    </body>
</html>
