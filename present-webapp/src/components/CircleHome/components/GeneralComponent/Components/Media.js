import React, { Component, PropTypes} from 'react';
import _ from 'lodash';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import Modal from '../../Modal';
import * as userActions from '../../../../../redux/actions/userActions';
import PhotoSlider from '../../../../common/PhotoSlider';
import UserDetails from '../../../../common/UserDetailsModal';
import userUtiles from '../../../../../core/userUtiles';

class Media extends Component {

  constructor(props){
    super(props);

    this.state={
      content:{}
    }
  }

  renderPhotoGridModal(media){
    let showMedia = media.map((item,index) => {
      return(
        <div key={index} className="small-4 medium-4 large-4 end columns">
            <img alt="content"
              className="grid-media-img cursor-poiner"
              src={item.content.content}
              onClick={() => this.setState({
                showSliderImg: true,
                content:item,
                chatImageVisible:false
              })}/>
        </div>
      )
    });

    return(
      <Modal show={this.state.chatImageVisible}
             closeModal={() => this.setState({chatImageVisible:false})}>
             <div className="show-all-media-container">
               <img src={require('../../../../../assets/images/close-btn-black@2x.png')}
                  className="grid-btn"
                  onClick={() => this.setState({chatImageVisible:false})}
                  alt="close"/>
                <div className="row show-all-media">
                 {showMedia}
               </div>
             </div>

      </Modal>
    )
  }

  showUserModalDetails(member){
    this.context.router.push(userUtiles.getUserLink(member));
  }

  render(){
    let {media} = this.props,
        showImg = <span/>,
        showFirstThree=[];

    if(media.length !== 0){
      //check how many photos availabe if there are more than 3 just show the first 3 photos
      if(media.length > 3){
        for (var i = 0; i < 3; i++) {
        showFirstThree.push(media[i]);
        }
      } else {
        showFirstThree = media;
      }

      showImg = showFirstThree.map((item, index) => {
        return (
          <div key={index} className="media-img container">
            <img alt="media"
              className="media-img cursor-poiner"
              src={item.content.content}
              onClick={() => this.setState({
                showSliderImg: true,
                content:item,
                chatImageVisible:false
              })}/>
          </div>
        )
      })
    }

    return(
      <div className="description-container">
      {this.state.showMemberModal ?
        <UserDetails
          currentUser={this.props.currentUser}
          user={this.state.member}
          showMemberModal={this.state.showMemberModal}
          onCloseModal={() => this.setState({showMemberModal:false})} /> : <span/>}
      {this.state.chatImageVisible ?
         this.renderPhotoGridModal(media) : <span/>}
      {this.state.showSliderImg ?
        <PhotoSlider
          showSliderImg={this.state.showSliderImg}
          media={media}
          content={this.state.content}
          onClosePhotoSlider={() => this.setState({showSliderImg:false})}
          showUserModalDetails={(user) => this.showUserModalDetails(user)}
          />
        : <span/>
      }
        <p className="title-gen">Media</p>
        <div className="general-media-container">
          {showImg}
        </div>
        {media.length > 3 ?
          <div className="text-center">
            <button onClick={() => this.setState({chatImageVisible:true})} className="show-more">See All</button>
          </div> : <span/>
        }

      </div>
    )
  }
}

Media.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
      currentUser:state.user.currentUser
    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators(userActions, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Media);
