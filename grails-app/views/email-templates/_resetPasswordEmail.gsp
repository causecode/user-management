<%@ page contentType="text/html"%>
<!DOCTYPE html>
<html lang="en">
<head>
<title></title>

</head>
<body>
    <div style="border: 1px solid black; -moz-border-radius: 5px; -webkit-border-radius: 5px;
            border-radius: 5px; widht: 100%; overflow: hidden;">
        <div style="background-color: black; height: 20px; color: white; width: 100%; padding: 20px 10px;
                font-weight: bold; letter-spacing: 1px; font-size: 20px; -moz-border-radius: 5px 5px 0px 0px;
                -webkit-border-radius: 5px 5px 0px 0px;">
            <span>Hello ${userInstance.fullName }</span>
        </div>
        <div style="padding: 10px;">
            <span>You (or someone pretending to be you) requested that your password be reset.<br />
                If you didn't make this request then ignore the email, no changes have been made to your account.<br />
                If you did make the request, then click <a href="${url}">here</a> to reset your password.
                <br /><br />
                Thank You <br />
                Team CauseCode.
            </span>
        </div>
    </div>
</body>
</html>