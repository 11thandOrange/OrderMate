# CustomOrderApp


#1 to make the build upload on any store you need to sign he app with the V1 sign key

   
(appSigner path)  sign --ks (path of the keystore file)  --v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert (path of the apk to be signed)

generally the path of the appsigner is **Library/Android/sdk/build-tools/31.0.0/apksigner**

e.g.
/Users/manjotsingh/Library/Android/sdk/build-tools/31.0.0/apksigner  sign 
--ks /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/Cert 
--v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/app/release/app-release.apk 



**#2** To get the data from the connectors you need to connect the app from the **preview in app market option >> connect the app** >> after this you
will get the data else you will not.



/Users/manjotsingh/Library/Android/sdk/build-tools/31.0.0/apksigner  sign
--ks /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/Cert
--v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/app/release/app-release.apk 
