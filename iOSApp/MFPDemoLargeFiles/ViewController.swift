//
//  ViewController.swift
//  MFPDemoLargeFiles
//
//  Created by Arik Shifer on 28/11/2016.
//  Copyright © 2016 IBM. All rights reserved.
//

import UIKit
import IBMMobileFirstPlatformFoundation

class ViewController: UIViewController {

    @IBOutlet weak var downloadBtn: UIButton!
    @IBOutlet weak var uploadBtn: UIButton!
    @IBOutlet weak var logoutBtn: UIButton!
    
    let largeFileName = "big_buck_bunny.mp4"
    let securityCheck = "UserLogin"
    let downloadEndpoint = "/adapters/MFPDemoLargeFilesAdapter/resource/download"
    let uploadEndpoint = "/adapters/MFPDemoLargeFilesAdapter/resource/uploader"
    
    lazy var urlSession: URLSession = {
        let configuration = URLSessionConfiguration.default
        let session = URLSession(configuration: configuration)
        return session
    }()
    
    var authToken:AccessToken?
    var downloadedFileUrl: URL?

    override func viewDidLoad() {
        super.viewDidLoad()
        // Register MFP challenge handler
        WLClient.sharedInstance().register(LoginChallengeHandler(securityCheck: securityCheck))

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    @IBAction func downloadAction(_ sender: Any) {
        let url = WLClient().serverUrl().appendingPathComponent(downloadEndpoint)
        downloadFile(fileUrl: url)
    }
    
    @IBAction func uploadAction(_ sender: Any) {
        if(self.downloadedFileUrl != nil) {
            let url = URL(string: WLClient().serverUrl().appendingPathComponent(uploadEndpoint).absoluteString.appending("?fileName=\(largeFileName)"))
            uploadFile(fileUrl: url!)
        } else {
            print("Couldn't find file to upload. Download file first!")
        }
    }
    
    @IBAction func logoutAction(_ sender: Any) {
        WLAuthorizationManager.sharedInstance().logout(securityCheck){ (error) -> Void in
            if(error == nil){
                print("Logged out of security check: \(self.securityCheck)")
            } else {
                print("Logout error: \(error!.localizedDescription)")
            }
        }
    }
    
    func downloadFile(fileUrl: URL) {
        
        // Create request
        var request = URLRequest(url:fileUrl)
        
        // Add MFP auth token to the request
        if let token = self.authToken {
            request.setValue(token.asAuthorizationRequestHeaderField, forHTTPHeaderField: "Authorization")
        }
        
        // Create download task
        let task = urlSession.downloadTask(with: request) { (tempLocalUrl, response, error) in
            if let tempLocalUrl = tempLocalUrl, error == nil {
                // Check if MFP authorization is required
                if (WLAuthorizationManager.sharedInstance().isAuthorizationRequired(for: response)) {
                    print ("Authorization required!")
                    // Find the authorization scope
                    let scope = WLAuthorizationManager.sharedInstance().resourceScope(from: response)
                    print("Requesting access token for scope: \(scope)")
                    // Obtain MFP access token
                    WLAuthorizationManager.sharedInstance().obtainAccessToken(forScope: scope) { (token, error) -> Void in
                        if(error == nil) {
                            self.authToken = token
                            print("Access token obtained. Resending request to download the large file with the access token");
                            self.downloadFile(fileUrl: fileUrl)
                        }else {
                            print("Failed to obtain access token: \(error!.localizedDescription)");
                        }
                    }
                } else {
                    if let statusCode = (response as? HTTPURLResponse)?.statusCode {
                        if (statusCode == 200) {
                            print("Successfully downloaded file")
                            // Create destination URL
                            let documentsUrl:URL =  FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first as URL!
                            self.downloadedFileUrl = documentsUrl.appendingPathComponent(self.largeFileName)
                            print("Copying from local temp file at: \(tempLocalUrl) to destination file at: \(self.downloadedFileUrl!)")
                            let fileManager = FileManager.default
                            do {
                                try fileManager.removeItem(at: self.downloadedFileUrl!)
                            } catch {
                                // Non-fatal: file probably doesn't exist
                            }
                            
                            do {
                                try fileManager.copyItem(at: tempLocalUrl, to: self.downloadedFileUrl!)
                            } catch (let writeError) {
                                print("Error creating a file \(self.downloadedFileUrl!) : \(writeError)")
                            }
                        } else {
                            print("Unexpected response. Status code: \(statusCode)")
                        }
                    }
                }
                
            } else {
                print("Error downloading file. Error description: %@", error?.localizedDescription ?? "");
            }
        }
        // Execute the download task
        task.resume()
    }

    func uploadFile(fileUrl: URL) {
        
        // Create post request
        var request = URLRequest(url: fileUrl)
        request.httpMethod = "POST"
        request.setValue("Keep-Alive", forHTTPHeaderField: "Connection")
        
        
        // Add MFP auth token to the request
        if let token = self.authToken {
            request.setValue(token.asAuthorizationRequestHeaderField, forHTTPHeaderField: "Authorization")
        }
        
        let uploadTask = urlSession.uploadTask(with: request as URLRequest, fromFile:self.downloadedFileUrl!, completionHandler: {(data,response,error) in
            
            if error == nil {
                // Check if MFP authorization is required
                if (WLAuthorizationManager.sharedInstance().isAuthorizationRequired(for: response)) {
                    print ("Authorization required!")
                    // Find the authorization scope
                    let scope = WLAuthorizationManager.sharedInstance().resourceScope(from: response)
                    print("Requesting access token for scope: \(scope)")
                    // Obtain MFP access token
                    WLAuthorizationManager.sharedInstance().obtainAccessToken(forScope: scope) { (token, error) -> Void in
                        if(error == nil) {
                            self.authToken = token
                            print("Access token obtained. Resending request to upload the large file with the access token");
                            self.uploadFile(fileUrl: fileUrl)
                        }else {
                            print("Failed to obtain access token: \(error!.localizedDescription)");
                        }
                    }
                    
                } else {
                    if let statusCode = (response as? HTTPURLResponse)?.statusCode {
                        if (statusCode == 200) {
                            print("Successfully uploaded file")
                        } else {
                            print("Unexpected response. Status code: \(statusCode)")
                        }
                    }
                }
                
            } else {
                print("Error uploading file. Error description: %@", error?.localizedDescription ?? "");
            }
            
        }
        )
        
        uploadTask.resume()
    }
}

