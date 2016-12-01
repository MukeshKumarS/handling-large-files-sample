//
//  LoginChallengeHandler.swift
//  DemoLargeFileDownload
//
//  Created by Arik Shifer on 20/11/2016.
//  Copyright © 2016 IBM. All rights reserved.
//

import Foundation
import IBMMobileFirstPlatformFoundation
 
class LoginChallengeHandler: SecurityCheckChallengeHandler {
    
    
    override init(securityCheck: String){
        super.init(securityCheck: securityCheck)
    }
    
    // handleChallenge
    override func handleChallenge(_ challenge: [AnyHashable : Any]!) {
        print("Challenged: ")
        self.submitChallengeAnswer(["username": "test", "password": "test"])
    }
   
    // handleSuccess
    override func handleSuccess(_ success: [AnyHashable : Any]!) {
         print("Challenge handleSuccess: ")
    }
    
    // handleFailure
    override func handleFailure(_ failure: [AnyHashable : Any]!) {
        print("Challenge failure: ")

    }
}
