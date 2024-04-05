//
//  MessageMetadataDisplayStrategy.swift
//  Present
//
//  Created by Patrick Niemeyer on 8/29/16.
//

import Foundation

/**
 Determine how dates, timestamps, and user name labels are applied to sequences of messages
 for display in the message list.
 */
/*
 Janete says:
 
 1) name label: show it for groups of comments made by the same person.
 
 2) timestamp label: I prefer Google hangouts where we show:
 -date stamp centered at the top of the group of messages (Aug 5, 2016 or Sep 7, 2016). No time stamp needed in these center date labels
 -show time stamp for other people's messages at the bottom left for groups of messages.
 -show time stamp for my messages at the bottom right for groups of messages
 -if groups of messages have gaps of more than 10 minutes the add another time stamp below the next group of messages,
 
 I think 10 minutes works fine for groups of messages.
 */
class MessageMetadataDisplayStrategy
{
    typealias MessageRange = [JSQMessageModel]
    /**
     A MessagePredicate is presented with a message, an absolute index, and a range
     containing at least one message. The predicate returns true if the message should
     be included in the range.
     */
    typealias MessagePredicate = ( _ message:JSQMessageModel, _ index:Int, _ range:MessageRange )->(Bool)
    
    let groupMessagesGapTime = 10 * MSEC_PER_MIN
    
    func annotateMessages( _ messages : [JSQMessageModel] )
    {
        if messages.isEmpty { return }
        
        let userGroups = partitionByPredicate(messages) { message, index, range in
            return message.author.userToken == range.first!.author.userToken
            //return message.senderId == range.first!.senderId
        }
        // Show username at top of group of messages by the same person
        for group in userGroups {
            group.first!.showUsername = true
        }
        
        // Group messages by time
        let timeGroups = partitionByPredicate(messages) { message, index, range in
            guard message.date.millisecondsSince1970 >= range.last!.date.millisecondsSince1970 else {
                logError("Dates are in descending order!")
                return false
            }
            
            return message.date.millisecondsSince1970 - range.last!.date.millisecondsSince1970 <= groupMessagesGapTime
        }
        // Show date at top of groups of messages within time grouping
        // Show timestamp at bottom of groups of messages within time grouping
        for group in timeGroups {
            group.first!.showDate = true
            group.last!.showTimestamp = true
        }
    }
    
    /**
     Find contiguous ranges messages matching the predicate.
     */
    func partitionByPredicate( _ messages : [JSQMessageModel], predicate : MessagePredicate ) -> [MessageRange]
    {
        var ranges = [MessageRange]()
        var range = MessageRange()
        
        for (index,message) in messages.enumerated()
        {
            // Every range contains at least one message
            if range.isEmpty {
                range.append(message)
                continue
            }
            
            // If the message matches the predicate add it to the existing range
            if !predicate( message, index, range ) {
                // else start a new range.
                ranges.append(range)
                range = MessageRange()
            }
            
            range.append(message)
        }
        
        // Complete the final range
        if !range.isEmpty { ranges.append(range) }
        return ranges
    }
}
