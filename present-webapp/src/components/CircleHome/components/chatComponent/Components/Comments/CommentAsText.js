import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../../../redux/actions/userActions';
import ApiHandler from '../../../../../../core/ApiHandler';
import moment from 'moment';
import CopyToClipboard from 'react-copy-to-clipboard';
import Linkify from 'react-linkify';
import PhotoSlider from '../../../../../common/PhotoSlider';
import UserDetails from '../../../../../common/UserDetailsModal';

class CommentAsText extends Component {

  constructor(props){

    super(props);
    this.state={
      content:{}
    }
  }

  showUserModalDetails(id){
    this.setState({chatImageVisible:false});
    let request = ApiHandler.attachHeader({userId: id});
    this.props.actions.loadRequestedUser(request).then(member => {
      this.setState({showMemberModal:true, member});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  render() {
    let chat = this.props.chat;
        return (
          <div className="m-co">
            <div className={chat.content ? "image-mas-container": "msg"} >
              <span className="username">{this.props.userName}</span>
              <span className="username time-stamp">{moment.unix(chat.creationTime/1000).format("h:mm A")}</span>
              <div>
                {chat.content ?
                  <img src={chat.content.content}
                    onClick={() => this.setState({chatImageVisible:true, content:chat})}
                    className="chat-image cursor-poiner" alt=""/> :
                    <span/> }
                  {this.state.showMemberModal ?
                    <UserDetails
                      currentUser={this.props.currentUser}
                      user={this.state.member}
                      showMemberModal={this.state.showMemberModal}
                      onCloseModal={() => this.setState({showMemberModal:false})} /> : <span/>}
                  {this.state.chatImageVisible ?
                    <PhotoSlider
                      fromComponent="comment"
                      content={this.state.content}
                      media={this.props.media}
                      showSliderImg={this.state.chatImageVisible}
                      showUserModalDetails={(id) => this.showUserModalDetails(id)}
                      onClosePhotoSlider={() => this.setState({chatImageVisible:false})}/>
                     : <span/>
                }
                <p>
                  {this.props.chat.comment.split('\n').map((item, key) => {
                    return <Linkify properties={item.indexOf('present.co/g/') === -1 ? {target: '_blank'} : {}} key={key}><span>{item}<br/></span></Linkify>
                  })}
                </p>

                </div>
              {this.props.isOwnerOfComment || this.props.isAdmin ?
                <div className="msgOptions-container">
                    <button className="dl-btn" onClick={() => this.props.deleteComment(chat)}>
                      <img src={require('../../../../../../assets/images/ic-delete-black-24-px@2x.png')} alt="" />
                    </button>
                  <CopyToClipboard
                        text={chat.comment}
                        onCopy={(text) => this.props.copyToClipboard()}>
                    <button className="dl-btn">
                      <img src={require('../../../../../../assets/images/ic-content-copy-black-24-px@2x.png')} alt="" />
                    </button>
                  </CopyToClipboard>
                </div> :
                <div/>
              }
          </div>
        </div>
        )
    }
}

function mapStateToProps(state, ownProps) {
    return {
      currentUser: state.user.currentUser
    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators(userActions, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(CommentAsText);
