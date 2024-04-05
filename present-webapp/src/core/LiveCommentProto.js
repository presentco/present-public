import protobuf from "protobufjs/light";

let LiveCommentProto = (function () {

  var Root  = protobuf.Root,
  Type  = protobuf.Type,
  Field = protobuf.Field;

  //request header
  var RequestHeader = new Type("RequestHeader")
      .add(new Field("clientUuid", 1, "string"));
      RequestHeader.add(new Field("requestUuid", 2 , "string"));
      RequestHeader.add(new Field("authorizationKey", 4, "string"));
      RequestHeader.add(new Field("apiVersion", 6 , "uint32"));
      RequestHeader.add(new Field("clientVersion", 7, "string"));
      RequestHeader.add(new Field("spaceId", 12, "string"));
      RequestHeader.add(new Field("platform", 5, "uint32"));

  //friend response
  var FriendResponse = new Type("FriendResponse")
      .add(new Field("user", 1, "UserResponse"));

  //user response
  var UserResponse = new Type("UserResponse")
      .add(new Field("id", 1, "string"));
      UserResponse.add(new Field("name", 2 , "string"));
      UserResponse.add(new Field("firstName", 7, "string"));
      UserResponse.add(new Field("photo", 3 , "string"));
      UserResponse.add(new Field("bio", 4 , "string"));
      UserResponse.add(new Field("interests", 5, "string"));
      UserResponse.add(new Field("signupLocation", 8 , "string"));
      UserResponse.add(new Field("link", 9 , "string"));
      UserResponse.add(new Field("friends", 6 , "FriendResponse"));


  //LiveCommentsRequest
  var LiveCommentsRequest = new Type("LiveCommentsRequest")
      .add(new Field("header", 1, "RequestHeader"));
      LiveCommentsRequest.add(new Field("groupId", 2, "string"));
      LiveCommentsRequest.add(new Field("userId", 3, "string"));
      LiveCommentsRequest.add(new Field("version", 4, "uint32"));


  var ContentResponse = new Type("ContentResponse")
      .add(new Field("uuid", 1 , "string"));
      ContentResponse.add(new Field("content", 8 , "string"));
      ContentResponse.add(new Field("contentThumbnail", 9, "string"));
      ContentResponse.add(new Field("contentType", 7 , "uint32"));

  //CommentResponse
  var CommentResponse = new Type("CommentResponse")
      CommentResponse.add(new Field("uuid", 1 , "string"));
      CommentResponse.add(new Field("groupId", 8, "string"));
      CommentResponse.add(new Field("author", 2 , "UserResponse"));
      CommentResponse.add(new Field("creationTime", 3, "uint64"));
      CommentResponse.add(new Field("likes", 6, "uint32"));
      CommentResponse.add(new Field("content", 7, "ContentResponse"));
      CommentResponse.add(new Field("devared", 9, "bool"));
      CommentResponse.add(new Field("index", 10, "uint32"));
      CommentResponse.add(new Field("comment", 5, "string"));


  var Present = new Root().define("Present").add(LiveCommentsRequest);
      Present.add(RequestHeader);
      Present.add(CommentResponse);
      Present.add(UserResponse);
      Present.add(FriendResponse);
      Present.add(ContentResponse);

  return {
    Present
  }

}());

export default LiveCommentProto;
