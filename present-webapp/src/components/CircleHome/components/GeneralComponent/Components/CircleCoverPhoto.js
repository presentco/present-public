import React, { Component} from 'react';
import _ from 'lodash';
import Modal from '../../Modal';

class CircleCoverPhoto extends Component {

  constructor(props){
    super(props);
    this.state={
      showCoverPhoto:false
    }
  }

  renderModalCoverPhoto(){
    return(
      <Modal show={this.state.showCoverPhoto}
        closeModal={() => this.setState({showCoverPhoto:false})}
        isCoverPhoto={true}>
        <img src={require('../../../../../assets/images/close-shape@2x.png')}
             className="x-btn cover-btn"
             onClick={() => this.setState({showCoverPhoto:false})}
             alt="close"/>
           <img alt="cover" src={this.props.currentGroup.cover.content}/>
    </Modal>
    )
  }

  render(){
    return(
      <div>
        <div className="cover-photo-container" onClick={() => this.setState({showCoverPhoto: true})}>
          <img
            alt={this.props.currentGroup.title}
            className="circle-photo"
            src={this.props.currentGroup.cover.content+"=w587-h330-n-rj"}/>
        </div>
        {this.state.showCoverPhoto ? this.renderModalCoverPhoto() : <span/>}
      </div>
    )
  }
}

export default CircleCoverPhoto;
