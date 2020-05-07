## Development

### Dev environment

1. Clone https://github.com/tsukaeru/rushfiles-zimlet.git in project root
1. Create folder in project root: .lib
1. Go to folder /opt/zimbra/lib/jars in host with deployed Zimbra 9.0.0
1. Put in .lib the next jars:
    * zimbra-charset.jar
    * zimbra-native.jar
    * zimbrastore.jar
    * zimbrasoap.jar
    * zimbracommon.jar
    * zimbraclient.jar
1. mvn verify

### Extension Installation

1. Build jar
1. Go to Zimbra console
1. Copy the jar to /opt/zimbra/lib/ext/rushfiles/rushfiles.jar
1. Execute:

       su - zimbra -c 'zmmailboxdctl restart'
1. Check deploying result (optional):

       cat /opt/zimbra/log/mailbox.log | grep -i rushfiles

### Testing

Tests live in ./tests folder. To make functional Servlet tests work you need to fill in an actual username and password in class ExtensionHttpServletTest.

API testing implemented by mock, Servlet testing - by real requests. We can treat API tests as unit-testing, Servlet tests - as functional-testing. (I do not see the point in long-running testing via requests the same functional twice.)

## API

### Authorization

#### Request

```http request
POST /service/extension/rushfiles/authorize
Content-Type: application/json

{
  "username": "hopster1222@gmail.com",
  "password": "L!-M/BBfol"
}
```

#### Response

You need to save username, primary_domain and domain_token in cookies. They need to be presented in every further request as cookies.

```json
{
  "status": "success",
  "username": "hopster1222@gmail.com",
  "primary_domain": "cloudfiles.jp",
  "domain_token": "11111"
}
```

### Authorization check

On every request backend will check the authorization. If it becomes invalid, you receive the next response:

```json
{
  "status": "error",
  "message": "unauthorized"
}
```

### All available shares

#### Request

```http request
POST /service/extension/rushfiles/get_all_shares
```

#### Response

```json
{
  "status": "success",
  "objects": [
    {
      "Id": "17b8cd708c5f41f0a7d30a7230612de2",
      "CompanyId": "39929886c09745e3bc98b9e85be7d0fb",
      "Name": "Comp Inc"
    },
    {
      "Id": "d94f8ed4c56e4f318edb41e5da8b064a",
      "CompanyId": "ca10b965-3b9f-4e5a-96a6-f10b3acea1b8",
      "Name": "hopster1222 - Home folder"
    }
  ]
}
```

### Get share contents

#### Request

```http request
POST /service/extension/rushfiles/get_share_contents

{
  "ShareId": "d94f8ed4c56e4f318edb41e5da8b064a"
}
```

#### Response

```json
{
  "status": "success",
  "objects": [
    {
      "IsFile": false,
      "InternalName": "a42a0704af704efd83e515f97cac7b70",
      "PublicName": "cats",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2"
    },
    {
      "IsFile": true,
      "InternalName": "b54e638b67664c688dd0cf28537bf191",
      "PublicName": "testfile.txt",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2"
    }
  ]
}
```

### Get folder contents

#### Request

```http request
POST /service/extension/rushfiles/get_folder_contents

{
  "ShareId": "17b8cd708c5f41f0a7d30a7230612de2",
  "InternalName": "a42a0704af704efd83e515f97cac7b70"
}
```

#### Response

```json
{
  "status": "success",
  "objects": [
    {
      "IsFile": true,
      "InternalName": "e7a1f0f3373640e3a22504cfcb786540",
      "PublicName": "eMail.png",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2"
    },
    {
      "IsFile": true,
      "InternalName": "3a1f3e331a1f47aeb03af1f294e21637",
      "PublicName": "DVD-Player.png",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2"
    }
  ]
}
```

### Creation links to files

#### Request

```http request
POST /service/extension/rushfiles/create_links_to_files

{
  "objects": [
    {
      "InternalName": "3a1f3e331a1f47aeb03af1f294e21637",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2"
    },
    {
      "InternalName": "3a1f3e331a1f47aeb03af1f294e21637",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2",
      "DaysToExpire": 10,
      "MaxUse": 5,
      "Message": "hello world",
      "Password": "123456"
    }
  ]
}
```

#### Response

```json
{
  "status": "success",
  "objects": [
    {
      "Link": "http://publiclink.com/123",
      "InternalName": "3a1f3e331a1f47aeb03af1f294e21637",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2",
      "DaysToExpire": null,
      "MaxUse": null,
      "Message": null,
      "Password": null
    },
    {
      "Link": "http://publiclink.com/123",
      "InternalName": "3a1f3e331a1f47aeb03af1f294e21637",
      "ShareId": "17b8cd708c5f41f0a7d30a7230612de2",
      "DaysToExpire": 10,
      "MaxUse": 5,
      "Message": "hello world",
      "Password": "123456"
    }
  ]
}
```