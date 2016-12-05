# MobileFirst Foundation v8 - handling of large files sample
This sample shows how a mobile application developer can download or upload large files to endpoints protected with IBM MobileFirst Foundation security. It offers implementation for Android and iOS Swift3 applications.

## Overview
It may happen that a mobile application developer will be required to download or upload very large files from/to a server.
For example allowing an app user to record a video using his mobile device and upload it to a secured space on the cloud.
When dealing with very large files it is important to perform the download or upload operations in chunks in both client and server implementations to avoid excessive memory consumption.

The IBM MobileFirst Foundation client API `WLResourceRequest` is not optimized for handling very large files since the request/response content is loaded entirely into memory. 
`WLResourceRequest` API for iOS offers a helper `WLResourceRequest.sendWithDelegate` API that allows downloading of large files. 

This sample shows a method for handling large files which can be applied to all platforms supported by IBM MobileFirst Foundation.

## Components
* A Foundation Java Adapter implementing two endpoints. One for downloading a large file and second for uploading a large file. 
<br />
The download endpoint connects to a backend server and downloads a large video file in chunks. Each chunk is written to the endpoint output stream to be read sequentially by the client app. 
<br />
The upload endpoint reads the client app uploaded large file in chunks and sequentially writes to a local file in the filesystem. 

* A Foundation Security Check implementing simple user login module to protect the Adapter endpoints.

* Android and iOS Swift3 mobile applications. 

## Mobile app pattern
The app issues a native request to download a large file from an endpoint protected with IBM MobileFirst Foundation security:
<br />
Android:
```bash
URLConnection conn = new URL(WLClient.getInstance().getServerUrl() + resourcePath).openConnection();
```
iOS:
```bash
var request = URLRequest(url:WLClient().serverUrl().appendingPathComponent(downloadEndpoint))
```

The app checks the response to find if authorization is required using the API:
<br />
Android:
```bash
WLAuthorizationManager.getInstance().isAuthorizationRequired(httpUrlConnection)
```
iOS:
```bash
WLAuthorizationManager.sharedInstance().isAuthorizationRequired(for: urlResponse)
```

In case authorization is required the app first queries the resource scope using the API:
<br />
Android:
```bash
WLAuthorizationManager.getInstance().getResourceScope((httpUrlConnection)
```
iOS:
```bash
WLAuthorizationManager.sharedInstance().resourceScope(from: urlResponse)
```


Then the app issues a request to obtain an access token for scope using the API:
<br />
Android:
```bash
WLAuthorizationManager.getInstance().obtainAccessToken(scope, new WLAccessTokenListener() {
    @Override
    public void onSuccess(AccessToken accessToken) {
        ...
    }
    @Override
    public void onFailure(WLFailResponse response) { 
        ...
    }
});
```

iOS:
```bash
WLAuthorizationManager.sharedInstance().obtainAccessToken(forScope: scope) { (token, error) -> Void in
    ...
}
```

When an access token is obtained it is cached for future calls and the app resends the native download request with the access token authorization http header.
<br />
Android:
```bash
httpUrlConnection.addRequestProperty("Authorization", accessToken.getAsAuthorizationRequestHeader());
```
iOS:
```bash
request.setValue(accessToken.asAuthorizationRequestHeaderField, forHTTPHeaderField: "Authorization")
```
The app then reads the large file in chunks and save it to the device local file system.

Uploading of a large file is implemented using a similar pattern. A native request is made to upload the file with the cached access token. In case token is invalid a request is made to obtain the token and the native upload request is repeated with the fresh token.
<br />
Further optimization can be considered by obtaining a fresh access token before issuing the native upload request. This will save time and network resources in case the cached token has expired.

## Prerequisites
This sample was generated on Mac OS. Developers using other development environments should adjust accordingly.

1. Installed [git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
2. Installed IBM MobileFirst Foundation v8 [development environment](https://mobilefirstplatform.ibmcloud.com/tutorials/en/foundation/8.0/setting-up-your-development-environment/).
3. Basic knowledge of [Foundation features and configuration](https://mobilefirstplatform.ibmcloud.com/tutorials/en/foundation/8.0/).
4. Android or iOS development environment.

## Running the sample
Open a terminal and navigate to your work directory.
Clone this sample git repository using the command:  

```bash
$ git clone git@github.com:mfpdev/handling-large-files-sample.git
```
Verify that you have an IBM MobileFirst Foundation server running on localhost.
<br />
Open a browser and login (admin/admin) to the IBM MobileFirst Foundation Operations Console:

```bash
http://localhost:9080/mfpconsole/
```

Build and deploy the sample Foundation Adapter. In the terminal navigate to the folder:
```
handling-large-files-sample/adapters/MFPDemoLargeFilesAdapter
```

Execute the Foundation CLI commands:
```bash
mfpdev adapter build
mfpdev adapter deploy
``` 

Build and deploy the sample Foundation Secuirty Check. In the terminal navigate to the folder:
```
handling-large-files-sample/adapters/UserLogin
```

Execute the Foundation CLI commands:
```bash
mfpdev adapter build
mfpdev adapter deploy
``` 

Import the App configurations to the Foundation server. In the Operations Console click the `Actions` dropdown menu.
Select `Import Application` and choose the file: 
```bash
handling-large-files-sample/config/export_applications_com.ibm.demo.mfpdemolargefiles_android_1.0.zip
```
Repeat the process to import the iOS app configuration file:
```bash
handling-large-files-sample/config/export_applications_com.ibm.demo.mfpdemolargefiles_ios_1.0.zip
```

To run the Android app using Android Studio open the android project located in folder:
```bash
handling-large-files-sample/androidApp/MFPDemoLargeFiles
```
Run the app in emulator and click `Download Large File` or `Upload Large File` buttons and watch the messages in Android Logcat console. Click the `Logout` button to force reathorization to occur.

To run the iOS app using Xcode navigate using the terminal to the iOS project located in folder:
```bash
handling-large-files-sample/iOSApp
```
Type the following comman to open the project in Xcode:
```bash
open MFPDemoLargeFiles.xcworkspace
```

Run the app in simulator and click `Download Large File` or `Upload Large File` buttons and watch the messages in the Xcode console. Click the `Logout` button to force reathorization to occur.


## License
Copyright 2016 IBM Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.